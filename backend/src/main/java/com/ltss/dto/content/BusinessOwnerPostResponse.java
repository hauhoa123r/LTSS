package com.ltss.dto.content;

import com.ltss.entity.content.PublicationStatus;

import java.time.Instant;

public record BusinessOwnerPostResponse(
        Long id,
        String title,
        String slug,
        String summary,
        String content,
        PublicationStatus status,
        Integer version,
        Instant publishedAt,
        Instant updatedAt
) {}
