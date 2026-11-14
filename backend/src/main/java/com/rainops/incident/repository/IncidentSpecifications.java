package com.rainops.incident.repository;

import com.rainops.incident.entity.Incident;
import com.rainops.incident.entity.IncidentPriority;
import com.rainops.incident.entity.IncidentStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class IncidentSpecifications {
    private IncidentSpecifications() { }

    public static Specification<Incident> matching(
            IncidentStatus status, IncidentPriority priority, String search) {
        return (root, query, builder) -> {
            List<Predicate> conditions = new ArrayList<>();
            if (status != null) {
                conditions.add(builder.equal(root.get("status"), status));
            }
            if (priority != null) {
                conditions.add(builder.equal(root.get("priority"), priority));
            }
            if (search != null && !search.isBlank()) {
                // Treat LIKE wildcards as literal user text.
                String text = search.strip().toLowerCase(Locale.ROOT)
                        .replace("!", "!!").replace("%", "!%").replace("_", "!_");
                String pattern = "%" + text + "%";
                conditions.add(builder.or(
                        builder.like(builder.lower(root.get("title")), pattern, '!'),
                        builder.like(builder.lower(root.get("district")), pattern, '!')));
            }
            return builder.and(conditions.toArray(Predicate[]::new));
        };
    }
}
