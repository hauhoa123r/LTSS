package com.ltss.features.analytics.dto;

import java.time.Instant;

public record RetentionStatusResponse(
        boolean deletionEnabled,
        long engagementEventCount,
        Instant oldestEngagementEventAt,
        long auditLogCount,
        Instant oldestAuditLogAt,
        String message
) {}
