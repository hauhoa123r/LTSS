package com.ltss.dto.quiz;

import java.math.BigDecimal;
import java.util.List;

public record QuizQuestionAuthorResponse(
        Long id,
        String content,
        String explanation,
        int displayOrder,
        BigDecimal points,
        List<QuizAnswerAuthorResponse> answers
) {}
