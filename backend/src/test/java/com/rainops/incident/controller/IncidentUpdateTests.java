package com.rainops.incident.controller;

import com.rainops.incident.entity.Incident;
import com.rainops.incident.entity.IncidentPriority;
import com.rainops.incident.entity.IncidentStatus;
import com.rainops.incident.repository.IncidentRepository;
import com.rainops.incident.service.IncidentService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class IncidentUpdateTests {
    @Autowired private WebApplicationContext context;
    @Autowired private IncidentService service;
    @Autowired private IncidentRepository repository;
    @Autowired private EntityManager entityManager;
    @Autowired private ObjectMapper objectMapper;
    private MockMvc mvc;
    private Incident original;

    private static final String UPDATE = """
            {"title":"Updated title","description":"Updated description",
             "district":"New district","locationDescription":"New location","priority":"HIGH"}
            """;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        original = service.createIncident("Original", "Description", "District", "Location",
                new BigDecimal("24.700000"), new BigDecimal("46.600000"), IncidentPriority.LOW);
        entityManager.flush();
        entityManager.clear();
        original = repository.findById(original.getId()).orElseThrow();
        entityManager.detach(original);
    }

    @Test
    void updatesDetailsAndClearsOmittedCoordinatesWhilePreservingIdentityAndStatus() throws Exception {
        long before = repository.count();
        String response = mvc.perform(put("/api/v1/incidents/{id}", original.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(UPDATE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("id").value(original.getId().toString()))
                .andExpect(jsonPath("title").value("Updated title"))
                .andExpect(jsonPath("priority").value("HIGH"))
                .andExpect(jsonPath("status").value("NEW"))
                .andReturn().getResponse().getContentAsString();
        entityManager.clear();
        Incident stored = repository.findById(original.getId()).orElseThrow();
        assertThat(repository.count()).isEqualTo(before);
        assertThat(stored.getDescription()).isEqualTo("Updated description");
        assertThat(stored.getDistrict()).isEqualTo("New district");
        assertThat(stored.getLocationDescription()).isEqualTo("New location");
        assertThat(stored.getLatitude()).isNull();
        assertThat(stored.getLongitude()).isNull();
        assertThat(stored.getStatus()).isEqualTo(IncidentStatus.NEW);
        assertThat(stored.getCreatedAt()).isEqualTo(original.getCreatedAt());
        assertThat(stored.getUpdatedAt()).isAfter(original.getUpdatedAt());
        Instant returnedTime = Instant.parse(objectMapper.readTree(response).get("updatedAt").asText());
        assertThat(returnedTime).isCloseTo(stored.getUpdatedAt(), org.assertj.core.api.Assertions.within(1, java.time.temporal.ChronoUnit.MICROS));
    }

    @Test
    void rejectsInvalidUpdateWithoutChangingTheIncident() throws Exception {
        mvc.perform(put("/api/v1/incidents/{id}", original.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(UPDATE.replace("Updated title", " ")))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/v1/incidents/{id}", original.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(UPDATE.replace("\"HIGH\"", "null")))
                .andExpect(status().isBadRequest());
        entityManager.clear();
        assertThat(repository.findById(original.getId()).orElseThrow().getTitle()).isEqualTo("Original");
    }

    @Test
    void returns404RatherThanCreatingForUnknownId() throws Exception {
        long before = repository.count();
        mvc.perform(put("/api/v1/incidents/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content(UPDATE))
                .andExpect(status().isNotFound());
        assertThat(repository.count()).isEqualTo(before);
    }
}

