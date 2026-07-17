package com.ltss.dto.administration;

import java.time.Instant;
import java.util.Map;

public record AuditLogResponse(
        Long id,
        Long actorUserId,
        String actorDisplayName,
        String actionCode,
        String actionLabel,
        String entityType,
        String entityTypeLabel,
        Long entityId,
        String entityDisplayName,
        Map<String, Object> oldValues,
        Map<String, Object> newValues,
        Map<String, Object> oldValuesDisplay,
        Map<String, Object> newValuesDisplay,
        String ipAddress,
        String requestId,
        Instant createdAt
) {}
