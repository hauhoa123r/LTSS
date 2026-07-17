package com.ltss.features.moderation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CancelModerationRequest(@NotNull @Min(0) Integer targetVersion) {
}
