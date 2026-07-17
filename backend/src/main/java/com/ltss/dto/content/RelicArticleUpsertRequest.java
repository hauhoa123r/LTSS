package com.ltss.dto.content;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RelicArticleUpsertRequest(
        @NotNull @Min(1) Long categoryId,
        @NotNull @Min(1) Long placeId,
        @NotBlank @Size(max = 250) String title,
        @Size(max = 700) String summary,
        @NotBlank @Size(max = 50000) String content,
        @Min(0) Integer version
) {}
