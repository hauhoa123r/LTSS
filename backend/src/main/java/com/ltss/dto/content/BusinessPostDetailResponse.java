package com.ltss.dto.content;

import java.time.Instant;
import java.util.List;

public record BusinessPostDetailResponse(
        Long id,
        String title,
        String slug,
        String summary,
        String content,
        BusinessResponse business,
        List<TagResponse> tags,
        List<ContentMediaResponse> media,
        Instant publishedAt,
        Integer version
) {
}
