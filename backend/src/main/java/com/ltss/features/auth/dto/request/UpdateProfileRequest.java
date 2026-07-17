package com.ltss.features.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank @Size(max = 150) String fullName,
        @NotBlank @Size(max = 150) String displayName,
        @Pattern(regexp = "[0-9]{10}") String phone,
        @Size(max = 500) String address
) {
}
