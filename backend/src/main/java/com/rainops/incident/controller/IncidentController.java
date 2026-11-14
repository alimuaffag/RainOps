package com.rainops.incident.controller;

import com.rainops.incident.dto.*;
import com.rainops.incident.entity.*;
import com.rainops.incident.service.IncidentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/incidents")
@RequiredArgsConstructor
public class IncidentController {
    private final IncidentService incidentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IncidentResponse createIncident(@Valid @RequestBody CreateIncidentRequest request) {
        return new IncidentResponse(incidentService.createIncident(
                request.getTitle(), request.getDescription(), request.getDistrict(),
                request.getLocationDescription(), request.getLatitude(),
                request.getLongitude(), request.getPriority()));
    }

    @GetMapping("/{id}")
    public IncidentResponse getIncident(@PathVariable UUID id) {
        return new IncidentResponse(incidentService.getIncident(id));
    }

    @GetMapping
    public IncidentPageResponse listIncidents(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) IncidentPriority priority,
            @RequestParam(required = false) @Size(max = 150) String search) {
        return new IncidentPageResponse(incidentService.listIncidents(page, size, status, priority, search));
    }

    @PutMapping("/{id}")
    public IncidentResponse updateIncident(
            @PathVariable UUID id, @Valid @RequestBody UpdateIncidentRequest request) {
        return new IncidentResponse(incidentService.updateIncident(
                id, request.getTitle(), request.getDescription(), request.getDistrict(),
                request.getLocationDescription(), request.getLatitude(),
                request.getLongitude(), request.getPriority()));
    }

    @PatchMapping("/{id}/status")
    public IncidentResponse changeStatus(
            @PathVariable UUID id, @Valid @RequestBody ChangeIncidentStatusRequest request) {
        return new IncidentResponse(incidentService.changeStatus(id, request.getStatus(), request.getNote()));
    }

    @GetMapping("/{id}/history")
    public List<IncidentStatusHistoryResponse> getHistory(@PathVariable UUID id) {
        return incidentService.getHistory(id);
    }
}
