package com.ltss.dto.content;

import com.ltss.entity.content.PublicationStatus;

import java.time.Instant;

public record RelicArticleResponse(
        Long id,
        Long categoryId,
        String categoryName,
        Long placeId,
        String placeName,
        String title,
        String slug,
        String summary,
        String content,
        PublicationStatus status,
        Integer version,
        Instant submittedAt,
        Instant publishedAt,
        Instant updatedAt
) {}
