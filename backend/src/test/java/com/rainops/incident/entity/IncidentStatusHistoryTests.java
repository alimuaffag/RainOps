package com.rainops.incident.entity;

import com.rainops.incident.service.IncidentService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class IncidentStatusHistoryTests {
    @Autowired private IncidentService incidentService;
    @Autowired private EntityManager entityManager;

    @Test
    void persistsCreationHistoryWithIncidentRelationshipAndGeneratedFields() {
        Incident incident = incidentService.createIncident(
                "History test", "Description", "District", "Entrance", null, null, null);
        IncidentStatusHistory history = new IncidentStatusHistory(
                incident, null, IncidentStatus.NEW, "Initial report");
        entityManager.persist(history);
        entityManager.flush();
        entityManager.clear();

        IncidentStatusHistory stored = entityManager.find(IncidentStatusHistory.class, history.getId());
        assertThat(stored).isNotNull();
        assertThat(stored.getIncident().getId()).isEqualTo(incident.getId());
        assertThat(stored.getIncident().getTitle()).isEqualTo("History test");
        assertThat(stored.getFromStatus()).isNull();
        assertThat(stored.getToStatus()).isEqualTo(IncidentStatus.NEW);
        assertThat(stored.getNote()).isEqualTo("Initial report");
        assertThat(stored.getChangedAt()).isNotNull();
    }
}
