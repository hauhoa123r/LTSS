package com.ltss.dto.quiz;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record QuestionUpsertRequest(
        @NotBlank @Size(max = 250) String content,
        @Size(max = 5000) String explanation,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 4, fraction = 2) BigDecimal points,
        @NotNull @Size(min = 2, max = 4) List<@Valid AnswerUpsertRequest> answers
) {}
