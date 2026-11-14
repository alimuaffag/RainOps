package com.rainops.incident.service;

import com.rainops.incident.dto.IncidentStatusHistoryResponse;
import com.rainops.incident.entity.*;
import com.rainops.incident.exception.IncidentNotFoundException;
import com.rainops.incident.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IncidentService {
    private final IncidentRepository incidentRepository;
    private final IncidentStatusHistoryRepository historyRepository;

    @Transactional
    public Incident createIncident(String title, String description, String district,
            String locationDescription, BigDecimal latitude, BigDecimal longitude, IncidentPriority priority) {
        Incident incident = new Incident();
        setDetails(incident, title, description, district, locationDescription, latitude, longitude,
                priority == null ? IncidentPriority.MEDIUM : priority);
        incidentRepository.save(incident);
        historyRepository.save(new IncidentStatusHistory(incident, null, IncidentStatus.NEW, null));
        return incident;
    }

    @Transactional(readOnly = true)
    public Incident getIncident(UUID id) {
        return incidentRepository.findById(id).orElseThrow(() -> new IncidentNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public Page<Incident> listIncidents(int page, int size,
            IncidentStatus status, IncidentPriority priority, String search) {
        PageRequest request = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        return incidentRepository.findAll(IncidentSpecifications.matching(status, priority, search), request);
    }

    @Transactional
    public Incident updateIncident(UUID id, String title, String description,
            String district, String locationDescription, BigDecimal latitude,
            BigDecimal longitude, IncidentPriority priority) {
        Incident incident = incidentRepository.findForUpdate(id)
                .orElseThrow(() -> new IncidentNotFoundException(id));
        setDetails(incident, title, description, district, locationDescription, latitude, longitude, priority);
        return incidentRepository.saveAndFlush(incident);
    }

    @Transactional
    public Incident changeStatus(UUID id, IncidentStatus target, String note) {
        Incident incident = incidentRepository.findForUpdate(id)
                .orElseThrow(() -> new IncidentNotFoundException(id));
        IncidentStatus previous = incident.getStatus();
        // Retrying an already-applied status does not create duplicate history.
        if (previous == target) {
            return incident;
        }
        incident.changeStatus(target);
        historyRepository.save(new IncidentStatusHistory(incident, previous, target, note));
        return incidentRepository.saveAndFlush(incident);
    }

    @Transactional(readOnly = true)
    public List<IncidentStatusHistoryResponse> getHistory(UUID id) {
        if (!incidentRepository.existsById(id)) {
            throw new IncidentNotFoundException(id);
        }
        return historyRepository.findByIncidentIdOrderByChangedAtAscIdAsc(id).stream()
                .map(IncidentStatusHistoryResponse::new).toList();
    }

    private void setDetails(Incident incident, String title, String description,
            String district, String locationDescription, BigDecimal latitude,
            BigDecimal longitude, IncidentPriority priority) {
        incident.setTitle(title);
        incident.setDescription(description);
        incident.setDistrict(district);
        incident.setLocationDescription(locationDescription);
        incident.setLatitude(latitude);
        incident.setLongitude(longitude);
        incident.setPriority(priority);
    }
}
