package com.ltss.dto.analytics;

import com.ltss.entity.analytics.EngagementTargetType;
import jakarta.validation.constraints.*;

import java.util.Map;

public record EngagementEventRequest(
        @NotBlank @Pattern(regexp = "[A-Z][A-Z0-9_]{0,39}") String eventTypeCode,
        @NotBlank @Size(max = 100) String sessionKey,
        @NotNull EngagementTargetType targetType,
        @NotNull @Min(1) Long targetId,
        @Size(max = 10) Map<String, @Size(max = 200) String> metadata
) {}
