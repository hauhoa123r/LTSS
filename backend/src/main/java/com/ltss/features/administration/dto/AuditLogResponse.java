package com.ltss.features.administration.dto;

import java.time.Instant;
import java.util.Map;

public record AuditLogResponse(
        Long id,
        Long actorUserId,
        String actionCode,
        String entityType,
        Long entityId,
        Map<String, Object> oldValues,
        Map<String, Object> newValues,
        String ipAddress,
        String requestId,
        Instant createdAt
) {}
