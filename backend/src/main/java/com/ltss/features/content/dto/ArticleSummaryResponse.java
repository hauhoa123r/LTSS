package com.ltss.features.content.dto;

import java.time.Instant;

public record ArticleSummaryResponse(
        Long id,
        String title,
        String slug,
        String summary,
        ArticleCategoryResponse category,
        LinkedPlaceResponse place,
        String coverUrl,
        Instant publishedAt
) {
}
