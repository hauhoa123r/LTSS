package com.ltss.dto.quiz;

import java.math.BigDecimal;
import java.util.List;

public record AttemptQuestionResponse(
        int questionOrder,
        Long questionId,
        String question,
        List<AttemptAnswerChoiceResponse> choices,
        Long selectedAnswerId,
        String selectedAnswer,
        String correctAnswer,
        String explanation,
        Boolean correct,
        BigDecimal awardedPoints
) {}
