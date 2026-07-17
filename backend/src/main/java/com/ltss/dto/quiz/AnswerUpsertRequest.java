package com.ltss.dto.quiz;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnswerUpsertRequest(
        @NotBlank @Size(max = 100) String content,
        boolean correct
) {}
