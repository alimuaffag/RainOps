package com.rainops.incident.dto;

import com.rainops.incident.entity.Incident;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
public class IncidentPageResponse {
    private final List<IncidentResponse> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;

    public IncidentPageResponse(Page<Incident> incidents) {
        content = incidents.getContent().stream().map(IncidentResponse::new).toList();
        page = incidents.getNumber();
        size = incidents.getSize();
        totalElements = incidents.getTotalElements();
        totalPages = incidents.getTotalPages();
    }
}
