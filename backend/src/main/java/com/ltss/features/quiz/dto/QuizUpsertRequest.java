package com.ltss.features.quiz.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record QuizUpsertRequest(
        @NotNull @Min(1) Long placeId,
        @NotBlank @Size(max = 250) String title,
        @Size(max = 10000) String description,
        @NotNull @Min(1) @Max(600) Integer timeLimitSeconds,
        @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal passingScorePercent,
        @NotNull @Size(min = 1, max = 100) List<@Valid QuestionUpsertRequest> questions,
        @Min(0) Integer version
) {}
