package com.ltss.features.administration.dto;

import com.ltss.features.auth.entity.UserStatus;
import java.time.Instant;
import java.util.List;

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
        List<String> effectiveRoles
) {}
