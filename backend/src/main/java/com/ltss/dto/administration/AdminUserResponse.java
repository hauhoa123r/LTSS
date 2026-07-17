package com.ltss.dto.administration;

import com.ltss.entity.auth.UserStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record AdminUserResponse(
        Long id,
        String fullName,
        String displayName,
        String email,
        String phone,
        UserStatus status,
        Instant emailVerifiedAt,
        Instant lastLoginAt,
        Instant lockedUntil,
        Instant deactivatedAt,
        Long deactivatedByUserId,
        Integer version,
        List<String> directRoles,
        List<String> effectiveRoles,
        Map<String, String> roleLabels
) {}
