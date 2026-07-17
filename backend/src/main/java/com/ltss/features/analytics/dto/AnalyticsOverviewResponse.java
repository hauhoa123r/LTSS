package com.ltss.features.analytics.dto;

import java.time.LocalDate;
import java.util.List;

public record AnalyticsOverviewResponse(
        LocalDate from,
        LocalDate to,
        Long businessId,
        String businessName,
        long totalEvents,
        long uniqueSessions,
        long authenticatedUsers,
        List<MetricCountResponse> byType,
        List<DailyCountResponse> daily
) {}
