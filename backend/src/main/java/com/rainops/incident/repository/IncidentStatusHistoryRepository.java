package com.rainops.incident.repository;

import com.rainops.incident.entity.IncidentStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface IncidentStatusHistoryRepository extends JpaRepository<IncidentStatusHistory, UUID> {
    List<IncidentStatusHistory> findByIncidentIdOrderByChangedAtAscIdAsc(UUID incidentId);
}
