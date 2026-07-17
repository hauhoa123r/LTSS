package com.ltss.dto.community;

import com.ltss.entity.community.*;
import java.time.Instant;
import java.util.List;

public record ReviewResponse(
        Long id, Long userId, String reviewerName, String reviewerAvatarUrl,
        ReviewTargetType targetType, Long targetId, short rating, String comment,
        ReviewStatus status, List<ReviewMediaResponse> media, ReviewReplyResponse reply,
        Integer version, Instant submittedAt, Instant publishedAt
) {}
