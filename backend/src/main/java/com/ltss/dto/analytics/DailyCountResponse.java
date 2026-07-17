package com.ltss.dto.analytics;

import java.time.LocalDate;

public record DailyCountResponse(LocalDate day, long value) {}
