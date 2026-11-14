package com.rainops.incident.entity;

import jakarta.persistence.*;
import com.rainops.incident.exception.InvalidStatusTransitionException;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "incidents")
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    @Setter
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    @Setter
    private String description;

    @Column(nullable = false, length = 100)
    @Setter
    private String district;

    @Column(name = "location_description", nullable = false, length = 500)
    @Setter
    private String locationDescription;

    @Column(precision = 9, scale = 6)
    @Setter
    private BigDecimal latitude;

    @Column(precision = 9, scale = 6)
    @Setter
    private BigDecimal longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Setter
    private IncidentPriority priority = IncidentPriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IncidentStatus status = IncidentStatus.NEW;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    public void changeStatus(IncidentStatus target) {
        Objects.requireNonNull(target, "Status is required");
        if (status == target) {
            return;
        }
        boolean allowed = switch (status) {
            case NEW -> target == IncidentStatus.IN_PROGRESS;
            case IN_PROGRESS -> target == IncidentStatus.RESOLVED;
            case RESOLVED -> target == IncidentStatus.IN_PROGRESS;
        };
        if (!allowed) {
            throw new InvalidStatusTransitionException(status, target);
        }
        status = target;
        resolvedAt = target == IncidentStatus.RESOLVED ? Instant.now() : null;
    }
    @PrePersist
    private void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    private void onUpdate() {
        updatedAt = Instant.now();
    }

}



