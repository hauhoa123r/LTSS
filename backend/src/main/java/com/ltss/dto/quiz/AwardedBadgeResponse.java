package com.ltss.dto.quiz;

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
