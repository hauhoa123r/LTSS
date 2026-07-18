package com.ltss.dto.analytics;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record BusinessStatisticsResponse(
        LocalDate startDate,
        LocalDate endDate,
        Instant generatedAt,
        long totalBusinesses,
        long totalActiveBusinesses,
        long pendingApprovals,
        long inactiveOrSuspendedAccounts,
        long rejectedAccounts,
        long currentPeriodRegistrations,
        long previousPeriodRegistrations,
        double growthPercent,
        List<MetricCountResponse> statusBreakdown,
        List<BusinessCategoryStatisticsResponse> categoryDistribution,
        List<BusinessAccountStatisticsResponse> accounts
) {}
