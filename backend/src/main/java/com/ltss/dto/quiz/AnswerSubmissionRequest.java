package com.ltss.dto.quiz;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AnswerSubmissionRequest(
        @Min(1) int questionOrder,
        @NotNull @Min(1) Long selectedAnswerId
) {}
