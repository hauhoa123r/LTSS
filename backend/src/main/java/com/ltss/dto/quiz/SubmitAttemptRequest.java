package com.ltss.dto.quiz;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SubmitAttemptRequest(
        @NotNull @Size(max = 100) List<@Valid AnswerSubmissionRequest> answers
) {}
