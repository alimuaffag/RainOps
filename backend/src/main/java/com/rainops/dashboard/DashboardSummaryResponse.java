package com.rainops.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.Map;

@Getter
@AllArgsConstructor
public class DashboardSummaryResponse {
    private final long total;
    private final Map<String, Long> byStatus;
    private final Map<String, Long> byPriority;
}
