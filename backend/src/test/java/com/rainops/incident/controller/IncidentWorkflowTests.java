package com.rainops.incident.controller;

import com.rainops.incident.entity.*;
import com.rainops.incident.service.IncidentService;
import com.rainops.incident.repository.IncidentStatusHistoryRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class IncidentWorkflowTests {
    @Autowired WebApplicationContext context;
    @Autowired IncidentService service;
    @Autowired IncidentStatusHistoryRepository history;
    @Autowired EntityManager entityManager;
    @Autowired ObjectMapper mapper;
    MockMvc mvc;

    @BeforeEach void setup() { mvc = MockMvcBuilders.webAppContextSetup(context).build(); }
    Incident create() {
        return service.createIncident("Workflow", "Description", "District", "Entrance",
                null, null, IncidentPriority.HIGH);
    }

    @Test void creationRecordsInitialStatusAndHistoryIsIsolated() throws Exception {
        Incident first = create();
        create();
        mvc.perform(get("/api/v1/incidents/{id}/history", first.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].incidentId").value(first.getId().toString()))
            .andExpect(jsonPath("$[0].toStatus").value("NEW"))
            .andExpect(jsonPath("$[0].changedAt").isNotEmpty());
        assertThat(history.findByIncidentIdOrderByChangedAtAscIdAsc(first.getId()).getFirst().getFromStatus()).isNull();
    }

    @Test void runsCompleteLifecycleAndReopensResolvedIncident() throws Exception {
        UUID id = create().getId();
        patchStatus(id, "IN_PROGRESS", "Started");
        mvc.perform(patch("/api/v1/incidents/{id}/status", id)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"RESOLVED\",\"note\":\"Done\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("status").value("RESOLVED"))
            .andExpect(jsonPath("resolvedAt").isNotEmpty());
        entityManager.clear();
        assertThat(service.getIncident(id).getResolvedAt()).isNotNull();
        patchStatus(id, "IN_PROGRESS", "Reopened");
        entityManager.clear();
        assertThat(service.getIncident(id).getResolvedAt()).isNull();
        mvc.perform(get("/api/v1/incidents/{id}/history", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(4))
            .andExpect(jsonPath("$[1].fromStatus").value("NEW"))
            .andExpect(jsonPath("$[2].toStatus").value("RESOLVED"))
            .andExpect(jsonPath("$[3].fromStatus").value("RESOLVED"))
            .andExpect(jsonPath("$[3].note").value("Reopened"));
    }

    @Test void rejectsSkippingWorkflowWithoutWritingHistory() throws Exception {
        UUID id = create().getId();
        mvc.perform(patch("/api/v1/incidents/{id}/status", id)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"RESOLVED\"}"))
            .andExpect(status().isConflict())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("status").value(409))
            .andExpect(jsonPath("detail").value("Cannot change incident status from NEW to RESOLVED"));
        assertThat(service.getIncident(id).getStatus()).isEqualTo(IncidentStatus.NEW);
        assertThat(service.getHistory(id)).hasSize(1);
    }

    @Test void sameStatusRetryDoesNotDuplicateHistory() throws Exception {
        UUID id = create().getId();
        patchStatus(id, "IN_PROGRESS", "Started");
        var updated = service.getIncident(id).getUpdatedAt();
        patchStatus(id, "IN_PROGRESS", "Retry");
        assertThat(service.getHistory(id)).hasSize(2);
        assertThat(service.getIncident(id).getUpdatedAt()).isEqualTo(updated);
    }

    @Test void invalidStatusRequestsReturnStructuredErrors() throws Exception {
        UUID id = create().getId();
        mvc.perform(patch("/api/v1/incidents/{id}/status", id)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("errors.status").exists());
        String longNote = mapper.writeValueAsString(java.util.Map.of("status", "IN_PROGRESS", "note", "x".repeat(1001)));
        mvc.perform(patch("/api/v1/incidents/{id}/status", id)
                .contentType(MediaType.APPLICATION_JSON).content(longNote))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("errors.note").exists());
        mvc.perform(patch("/api/v1/incidents/{id}/status", id)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"UNKNOWN\"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("detail").exists());
        assertThat(service.getHistory(id)).hasSize(1);
    }

    @Test void missingHistoryAndStatusReturn404() throws Exception {
        UUID absent = UUID.randomUUID();
        mvc.perform(get("/api/v1/incidents/{id}/history", absent))
            .andExpect(status().isNotFound()).andExpect(jsonPath("status").value(404));
        mvc.perform(patch("/api/v1/incidents/{id}/status", absent)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"IN_PROGRESS\"}"))
            .andExpect(status().isNotFound());
    }

    @Test void detailUpdatePreservesResolvedStateAndHistory() throws Exception {
        UUID id = create().getId();
        service.changeStatus(id, IncidentStatus.IN_PROGRESS, null);
        service.changeStatus(id, IncidentStatus.RESOLVED, null);
        entityManager.clear();
        var resolved = service.getIncident(id).getResolvedAt();
        mvc.perform(put("/api/v1/incidents/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Edited","description":"Updated","district":"New district",
                     "locationDescription":"New entrance","priority":"LOW"}
                    """))
            .andExpect(status().isOk()).andExpect(jsonPath("status").value("RESOLVED"));
        entityManager.clear();
        assertThat(service.getIncident(id).getResolvedAt()).isEqualTo(resolved);
        assertThat(service.getHistory(id)).hasSize(3);
    }

    @Test void dashboardReflectsCountsAcrossStatusChanges() throws Exception {
        var before = mapper.readTree(mvc.perform(get("/api/v1/dashboard/summary")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        Incident incident = create();
        service.changeStatus(incident.getId(), IncidentStatus.IN_PROGRESS, null);
        service.changeStatus(incident.getId(), IncidentStatus.RESOLVED, null);
        mvc.perform(get("/api/v1/dashboard/summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("total").value(before.get("total").asInt() + 1))
            .andExpect(jsonPath("byStatus.RESOLVED").value(before.get("byStatus").get("RESOLVED").asInt() + 1))
            .andExpect(jsonPath("byStatus.NEW").value(before.get("byStatus").get("NEW").asInt()))
            .andExpect(jsonPath("byPriority.HIGH").value(before.get("byPriority").get("HIGH").asInt() + 1));
    }

    @Test void validationAndUnknownFieldsHaveUsefulErrors() throws Exception {
        mvc.perform(get("/api/v1/incidents").param("size", "101"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("errors.size").exists());
        mvc.perform(post("/api/v1/incidents").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("errors.title").exists());
        mvc.perform(post("/api/v1/incidents").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"Title","description":"Description","district":"District",
                     "locationDescription":"Location","status":"RESOLVED"}
                    """))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/incidents/not-a-uuid"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("status").value(400));
    }

    private void patchStatus(UUID id, String target, String note) throws Exception {
        mvc.perform(patch("/api/v1/incidents/{id}/status", id).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(java.util.Map.of("status", target, "note", note))))
            .andExpect(status().isOk()).andExpect(jsonPath("status").value(target));
    }
}
