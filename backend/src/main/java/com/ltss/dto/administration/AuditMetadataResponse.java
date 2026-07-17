package com.ltss.dto.administration;

import java.util.Map;

public record AuditMetadataResponse(
        Map<String, String> actionLabels,
        Map<String, String> entityTypeLabels
) {}
