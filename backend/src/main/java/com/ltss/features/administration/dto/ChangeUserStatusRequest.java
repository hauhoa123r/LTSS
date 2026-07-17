package com.ltss.features.administration.dto;

import com.ltss.features.auth.entity.UserStatus;
import jakarta.validation.constraints.*;

public record ChangeUserStatusRequest(
        @NotNull UserStatus status,
        @NotBlank @Size(max = 500) String reason,
        @NotNull @Min(0) Integer version
) {}
