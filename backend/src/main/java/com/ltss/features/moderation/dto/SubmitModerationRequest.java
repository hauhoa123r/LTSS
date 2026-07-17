package com.ltss.features.moderation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SubmitModerationRequest(
        @NotNull @Min(0) Integer targetVersion,
        @Size(max = 1000) String note
) {
}
