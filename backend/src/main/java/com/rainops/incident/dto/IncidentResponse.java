package com.rainops.incident.dto;

import com.rainops.incident.entity.Incident;
import com.rainops.incident.entity.IncidentPriority;
import com.rainops.incident.entity.IncidentStatus;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
public class IncidentResponse {
    private final UUID id;
    private final String title;
    private final String description;
    private final String district;
    private final String locationDescription;
    private final BigDecimal latitude;
    private final BigDecimal longitude;
    private final IncidentPriority priority;
    private final IncidentStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Instant resolvedAt;

    public IncidentResponse(Incident incident) {
        id = incident.getId();
        title = incident.getTitle();
        description = incident.getDescription();
        district = incident.getDistrict();
        locationDescription = incident.getLocationDescription();
        latitude = incident.getLatitude();
        longitude = incident.getLongitude();
        priority = incident.getPriority();
        status = incident.getStatus();
        createdAt = incident.getCreatedAt();
        updatedAt = incident.getUpdatedAt();
        resolvedAt = incident.getResolvedAt();
    }
}
