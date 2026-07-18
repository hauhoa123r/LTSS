package com.ltss.dto.administration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetUserPasswordRequest(@NotBlank @Size(max = 500) String reason) {}
