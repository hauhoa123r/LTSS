package com.ltss.features.quiz.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SubmitAttemptRequest(
        @NotNull @Size(max = 100) List<@Valid AnswerSubmissionRequest> answers
) {}
