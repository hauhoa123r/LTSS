package com.ltss.features.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank @Size(max = 512) String token,
        @NotBlank @Size(min = 8, max = 32) String newPassword
) {
}
