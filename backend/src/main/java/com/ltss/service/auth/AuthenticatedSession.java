package com.ltss.service.auth;

import com.ltss.dto.auth.response.AuthResponse;

public record AuthenticatedSession(AuthResponse response, String refreshToken) {
}
