package com.ltss.dto.content;

import java.time.Instant;

public record ArticleCategoryManagementResponse(
        Long id,
        String name,
        String slug,
        String description,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {}
