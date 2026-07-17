package com.ltss.features.analytics.dto;

import java.time.LocalDate;

public record DailyCountResponse(LocalDate day, long value) {}
