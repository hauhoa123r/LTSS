package com.ltss.dto.administration;

import jakarta.validation.constraints.*;

public record RoleChangeRequest(@NotBlank @Size(max = 500) String reason) {}
