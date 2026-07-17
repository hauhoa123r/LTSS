package com.ltss.features.auth.dto.response;

import java.time.Instant;

public record AuthResponse(
        String accessToken,
        String tokenType,
        Instant expiresAt,
        ProfileResponse user
) {
}
