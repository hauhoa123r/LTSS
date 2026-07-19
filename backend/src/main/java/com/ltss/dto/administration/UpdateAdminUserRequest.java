package com.ltss.dto.administration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateAdminUserRequest(
        @NotBlank @Size(max = 150) String fullName,
        @NotBlank @Size(max = 150) String displayName,
        @Size(max = 20) @Pattern(regexp = "^$|[0-9]{10}", message = "Phone must contain exactly 10 digits") String phone,
        @Size(max = 500) String address,
        @NotBlank @Size(max = 500) String reason,
        @NotNull
        Integer version
) {}
