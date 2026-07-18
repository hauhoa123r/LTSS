package com.ltss.dto.analytics;

public record BusinessCategoryStatisticsResponse(
        Long categoryId,
        String categoryName,
        String categorySlug,
        long totalBusinesses,
        long activeBusinesses,
        long pendingBusinesses,
        long inactiveOrSuspendedBusinesses,
        double percentage
) {}
