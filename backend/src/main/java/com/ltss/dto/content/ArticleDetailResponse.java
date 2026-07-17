package com.ltss.dto.content;

import java.time.Instant;
import java.util.List;

public record ArticleDetailResponse(
        Long id,
        String title,
        String slug,
        String summary,
        String content,
        ArticleCategoryResponse category,
        LinkedPlaceResponse place,
        Long eventId,
        List<ContentMediaResponse> media,
        Instant publishedAt,
        Integer version
) {
}
