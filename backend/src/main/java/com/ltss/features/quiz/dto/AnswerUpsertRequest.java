package com.ltss.features.quiz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnswerUpsertRequest(
        @NotBlank @Size(max = 100) String content,
        boolean correct
) {}
