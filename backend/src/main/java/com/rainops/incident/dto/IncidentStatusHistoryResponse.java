package com.rainops.incident.dto;

import com.rainops.incident.entity.IncidentStatus;
import com.rainops.incident.entity.IncidentStatusHistory;
import lombok.Getter;
import java.time.Instant;
import java.util.UUID;

@Getter
public class IncidentStatusHistoryResponse {
    private final UUID id;
    private final UUID incidentId;
    private final IncidentStatus fromStatus;
    private final IncidentStatus toStatus;
    private final String note;
    private final Instant changedAt;

    public IncidentStatusHistoryResponse(IncidentStatusHistory history) {
        id = history.getId();
        incidentId = history.getIncident().getId();
        fromStatus = history.getFromStatus();
        toStatus = history.getToStatus();
        note = history.getNote();
        changedAt = history.getChangedAt();
    }
}
