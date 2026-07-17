package com.ltss.dto.administration;

import com.ltss.entity.auth.UserStatus;
import jakarta.validation.constraints.*;

public record ChangeUserStatusRequest(
        @NotNull UserStatus status,
        @NotBlank @Size(max = 500) String reason,
        @NotNull @Min(0) Integer version
) {}
