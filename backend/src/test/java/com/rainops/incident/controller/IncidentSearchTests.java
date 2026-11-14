package com.rainops.incident.controller;

import com.rainops.incident.entity.IncidentPriority;
import com.rainops.incident.service.IncidentService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class IncidentSearchTests {
    @Autowired private WebApplicationContext context;
    @Autowired private IncidentService service;
    @Autowired private EntityManager entityManager;
    private MockMvc mvc;
    private String marker;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        marker = "test-" + UUID.randomUUID();
        service.createIncident(marker + " Flood", "Description", "النزهة", "Entrance",
                null, null, IncidentPriority.HIGH);
        service.createIncident("Road water", "Description", marker + " District", "Entrance",
                null, null, IncidentPriority.LOW);
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void searchesTitleOrDistrictIgnoringCaseAndOuterWhitespaceWithPagination() throws Exception {
        mvc.perform(get("/api/v1/incidents").param("search", "  " + marker.toUpperCase() + "  ")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("content.length()").value(1))
                .andExpect(jsonPath("totalElements").value(2))
                .andExpect(jsonPath("totalPages").value(2));
    }

    @Test
    void combinesSearchStatusAndPriority() throws Exception {
        mvc.perform(get("/api/v1/incidents").param("search", marker)
                        .param("status", "NEW").param("priority", "HIGH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("totalElements").value(1))
                .andExpect(jsonPath("content[0].title").value(marker + " Flood"));
        mvc.perform(get("/api/v1/incidents").param("search", marker).param("status", "RESOLVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("totalElements").value(0))
                .andExpect(jsonPath("content").isEmpty());
    }

    @Test
    void searchesArabicDistrictAndTreatsWildcardsLiterally() throws Exception {
        service.createIncident("Literal match", "Description", marker + " النزهة %_!", "Entrance",
                null, null, IncidentPriority.HIGH);
        mvc.perform(get("/api/v1/incidents").param("search", marker + " النزهة %_!"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("totalElements").value(1))
                .andExpect(jsonPath("content[0].title").value("Literal match"));
        mvc.perform(get("/api/v1/incidents").param("search", marker + "%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("totalElements").value(0));
    }

    @Test
    void ignoresBlankSearch() throws Exception {
        long count = entityManager.createQuery("select count(i) from Incident i", Long.class).getSingleResult();
        mvc.perform(get("/api/v1/incidents").param("search", "   "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("totalElements").value(Math.toIntExact(count)));
    }

    @Test
    void rejectsInvalidFiltersAndOversizedSearch() throws Exception {
        mvc.perform(get("/api/v1/incidents").param("status", "UNKNOWN"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/incidents").param("priority", "URGENT"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/incidents").param("search", "x".repeat(151)))
                .andExpect(status().isBadRequest());
    }
}
