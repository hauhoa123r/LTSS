package com.ltss.dto.auth.response;

import java.time.Instant;

public record AuthResponse(
        String accessToken,
        String tokenType,
        Instant expiresAt,
        ProfileResponse user
) {
}
