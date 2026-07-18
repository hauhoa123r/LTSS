package com.ltss.dto.analytics;

import java.time.Instant;
import java.util.List;

public record MonumentVisitResponse(
        Long placeId,
        String name,
        String slug,
        String address,
        long visits,
        long uniqueSessions,
        long authenticatedVisitors,
        double growthPercent,
        double averagePerDay,
        Instant lastVisitAt,
        List<DailyCountResponse> trend
) {}
