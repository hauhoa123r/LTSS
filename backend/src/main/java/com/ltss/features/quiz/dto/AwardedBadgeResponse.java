package com.ltss.features.quiz.dto;

import java.time.Instant;

public record AwardedBadgeResponse(
        Long id,
        String code,
        String name,
        String description,
        String iconUrl,
        Instant awardedAt,
        Long quizId,
        String quizTitle
) {}
