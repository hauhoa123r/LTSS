package com.ltss.dto.analytics;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record MonumentStatisticsResponse(
        LocalDate startDate,
        LocalDate endDate,
        MonumentGranularity granularity,
        Instant generatedAt,
        long totalVisits,
        long previousTotalVisits,
        double growthPercent,
        double averageVisitsPerDay,
        LocalDate peakVisitDay,
        long peakVisitCount,
        long uniqueSessions,
        long authenticatedVisitors,
        long activeMonuments,
        String mostVisitedMonument,
        List<DailyCountResponse> trends,
        List<DailyCountResponse> dailyTrends,
        List<MonumentVisitResponse> monuments
) {}
