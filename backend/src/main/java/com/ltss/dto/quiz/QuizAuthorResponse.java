package com.ltss.dto.quiz;

import com.ltss.entity.quiz.QuizStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record QuizAuthorResponse(
        Long id,
        Long placeId,
        String placeName,
        String title,
        String description,
        int timeLimitSeconds,
        BigDecimal passingScorePercent,
        QuizStatus status,
        Integer version,
        Instant submittedAt,
        Instant publishedAt,
        List<QuizQuestionAuthorResponse> questions
) {}
