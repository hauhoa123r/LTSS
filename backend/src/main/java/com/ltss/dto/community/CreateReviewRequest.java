package com.ltss.dto.community;

import jakarta.validation.constraints.*;
import java.util.List;

public record CreateReviewRequest(
        @Min(1) @Max(5) int rating,
        @NotBlank @Size(min = 20, max = 5000) String comment,
        @Size(max = 3) List<@Min(1) Long> mediaAssetIds
) {}
