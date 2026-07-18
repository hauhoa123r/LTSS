package com.ltss.dto.analytics;

import java.time.Instant;

public record BusinessAccountStatisticsResponse(
        Long businessId,
        String businessName,
        String placeSlug,
        String categoryName,
        String categorySlug,
        String status,
        Instant createdAt,
        Instant approvedAt
) {}
