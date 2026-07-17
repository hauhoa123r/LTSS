package com.ltss.features.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank @Size(max = 32) String currentPassword,
        @NotBlank @Pattern(regexp = "[0-9]{6}") String otp,
        @NotBlank @Size(min = 8, max = 32) String newPassword
) {
}
