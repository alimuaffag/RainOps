package com.rainops.dashboard;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final JdbcTemplate jdbc;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse summary() {
        // One query gives all counters the same database snapshot.
        return jdbc.queryForObject("""
            SELECT count(*) AS total,
                   count(*) FILTER (WHERE status = 'NEW') AS new_count,
                   count(*) FILTER (WHERE status = 'IN_PROGRESS') AS progress_count,
                   count(*) FILTER (WHERE status = 'RESOLVED') AS resolved_count,
                   count(*) FILTER (WHERE priority = 'LOW') AS low_count,
                   count(*) FILTER (WHERE priority = 'MEDIUM') AS medium_count,
                   count(*) FILTER (WHERE priority = 'HIGH') AS high_count
            FROM incidents
            """, (rs, row) -> new DashboardSummaryResponse(
                rs.getLong("total"),
                Map.of("NEW", rs.getLong("new_count"),
                       "IN_PROGRESS", rs.getLong("progress_count"),
                       "RESOLVED", rs.getLong("resolved_count")),
                Map.of("LOW", rs.getLong("low_count"),
                       "MEDIUM", rs.getLong("medium_count"),
                       "HIGH", rs.getLong("high_count"))));
    }
}
