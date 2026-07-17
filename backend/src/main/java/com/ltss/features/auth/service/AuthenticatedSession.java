package com.ltss.features.auth.service;

import com.ltss.features.auth.dto.response.AuthResponse;

public record AuthenticatedSession(AuthResponse response, String refreshToken) {
}
