package com.ltss.features.administration.dto;

import jakarta.validation.constraints.*;

public record RoleChangeRequest(@NotBlank @Size(max = 500) String reason) {}
