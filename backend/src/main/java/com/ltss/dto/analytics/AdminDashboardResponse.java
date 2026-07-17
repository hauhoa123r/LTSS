package com.ltss.dto.analytics;

import java.util.Map;

public record AdminDashboardResponse(
        Map<String, Long> usersByStatus,
        long publishedPlaces,
        long activeBusinesses,
        AnalyticsOverviewResponse engagement
) {}
