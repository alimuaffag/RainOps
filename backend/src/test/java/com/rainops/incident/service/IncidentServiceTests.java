package com.rainops.incident.service;

import com.rainops.incident.entity.Incident;
import com.rainops.incident.entity.IncidentPriority;
import com.rainops.incident.entity.IncidentStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class IncidentServiceTests {

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private EntityManager entityManager;

    @Test
    void createsAndPersistsNewIncidentWithDefaults() {
        Incident created = incidentService.createIncident(
                "Water accumulation", "Water blocks the entrance", "Al Nuzhah",
                "School entrance", null, null, null);

        entityManager.flush();
        entityManager.clear();

        Incident stored = entityManager.find(Incident.class, created.getId());
        assertThat(stored).isNotNull();
        assertThat(stored.getTitle()).isEqualTo("Water accumulation");
        assertThat(stored.getDescription()).isEqualTo("Water blocks the entrance");
        assertThat(stored.getDistrict()).isEqualTo("Al Nuzhah");
        assertThat(stored.getLocationDescription()).isEqualTo("School entrance");
        assertThat(stored.getStatus()).isEqualTo(IncidentStatus.NEW);
        assertThat(stored.getPriority()).isEqualTo(IncidentPriority.MEDIUM);
        assertThat(stored.getCreatedAt()).isNotNull();
        assertThat(stored.getUpdatedAt()).isEqualTo(stored.getCreatedAt());
        assertThat(stored.getResolvedAt()).isNull();
    }
}
