package com.ltss.dto.place;

import java.time.Instant;

public record PlaceCategoryManagementResponse(
        Long id,
        String name,
        String slug,
        String description,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {}
