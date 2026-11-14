package com.rainops.incident.controller;

import com.rainops.incident.entity.Incident;
import com.rainops.incident.repository.IncidentRepository;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class IncidentControllerTests {
    @Autowired private WebApplicationContext context;
    @Autowired private IncidentRepository repository;
    @Autowired private EntityManager entityManager;
    @Autowired private ObjectMapper objectMapper;
    private MockMvc mvc;

    private static final String VALID_REQUEST = """
            {
              "title": "تجمع مياه",
              "description": "المياه تعيق مدخل المدرسة",
              "district": "النزهة",
              "locationDescription": "أمام المدرسة"
            }
            """;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void createsIncidentAndReturnsGeneratedFields() throws Exception {
        String body = mvc.perform(post("/api/v1/incidents")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("title").value("تجمع مياه"))
                .andExpect(jsonPath("status").value("NEW"))
                .andExpect(jsonPath("priority").value("MEDIUM"))
                .andExpect(jsonPath("id").isNotEmpty())
                .andExpect(jsonPath("createdAt").isNotEmpty())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        UUID id = UUID.fromString(objectMapper.readTree(body).get("id").asText());
        entityManager.flush();
        entityManager.clear();
        Incident stored = repository.findById(id).orElseThrow();
        assertThat(stored.getTitle()).isEqualTo("تجمع مياه");
    }

    @Test
    void rejectsBlankTitleWithoutSaving() throws Exception {
        assertRejected(VALID_REQUEST.replace("تجمع مياه", "   "));
    }

    @Test
    void rejectsUnpairedCoordinatesWithoutSaving() throws Exception {
        assertRejected(VALID_REQUEST.replace("\"title\":", "\"latitude\": 24.7, \"title\":"));
    }

    @Test
    void rejectsUnknownPriorityWithoutSaving() throws Exception {
        assertRejected(VALID_REQUEST.replace("\"title\":", "\"priority\": \"URGENT\", \"title\":"));
    }

    @Test
    void retrievesCreatedIncidentById() throws Exception {
        String body = mvc.perform(post("/api/v1/incidents")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        String id = objectMapper.readTree(body).get("id").asText();
        entityManager.flush();
        entityManager.clear();

        mvc.perform(get("/api/v1/incidents/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("id").value(id))
                .andExpect(jsonPath("title").value("تجمع مياه"))
                .andExpect(jsonPath("district").value("النزهة"))
                .andExpect(jsonPath("status").value("NEW"));
    }

    @Test
    void returnsNotFoundForUnknownId() throws Exception {
        UUID absentId = UUID.randomUUID();
        assertThat(repository.existsById(absentId)).isFalse();
        mvc.perform(get("/api/v1/incidents/{id}", absentId))
                .andExpect(status().isNotFound());
    }

    @Test
    void returnsBadRequestForMalformedId() throws Exception {
        mvc.perform(get("/api/v1/incidents/not-a-uuid"))
                .andExpect(status().isBadRequest());
    }
    @Test
    void listsIncidentsInPagesWithoutRepeatingTheFirstItem() throws Exception {
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/api/v1/incidents")
                            .contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
                    .andExpect(status().isCreated());
        }
        entityManager.flush();
        entityManager.clear();
        int total = Math.toIntExact(repository.count());
        String first = mvc.perform(get("/api/v1/incidents").param("page", "0").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("content.length()").value(1))
                .andExpect(jsonPath("page").value(0))
                .andExpect(jsonPath("size").value(1))
                .andExpect(jsonPath("totalElements").value(total))
                .andExpect(jsonPath("totalPages").value(total))
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        String second = mvc.perform(get("/api/v1/incidents").param("page", "1").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("content.length()").value(1))
                .andExpect(jsonPath("page").value(1))
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        var firstItem = objectMapper.readTree(first).get("content").get(0);
        var secondItem = objectMapper.readTree(second).get("content").get(0);
        assertThat(firstItem.get("id").asText()).isNotEqualTo(secondItem.get("id").asText());
        assertThat(java.time.Instant.parse(firstItem.get("createdAt").asText()))
                .isAfterOrEqualTo(java.time.Instant.parse(secondItem.get("createdAt").asText()));
    }

    @Test
    void usesDefaultPageSizeAndReturnsEmptyContentBeyondLastPage() throws Exception {
        mvc.perform(get("/api/v1/incidents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("page").value(0))
                .andExpect(jsonPath("size").value(20));
        mvc.perform(get("/api/v1/incidents")
                        .param("page", Long.toString(repository.count() + 1)).param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("content").isEmpty());
    }

    @Test
    void rejectsInvalidPagination() throws Exception {
        mvc.perform(get("/api/v1/incidents").param("page", "-1"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/incidents").param("size", "0"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/incidents").param("size", "101"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/incidents").param("page", "abc"))
                .andExpect(status().isBadRequest());
    }
    private void assertRejected(String body) throws Exception {
        long before = repository.count();
        mvc.perform(post("/api/v1/incidents")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        assertThat(repository.count()).isEqualTo(before);
    }
}


