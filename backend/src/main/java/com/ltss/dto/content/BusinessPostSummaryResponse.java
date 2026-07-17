package com.ltss.dto.content;

import java.time.Instant;
import java.util.List;

public record BusinessPostSummaryResponse(
        Long id,
        String title,
        String slug,
        String summary,
        Long businessId,
        String businessName,
        String coverUrl,
        List<TagResponse> tags,
        Instant publishedAt
) {
}
