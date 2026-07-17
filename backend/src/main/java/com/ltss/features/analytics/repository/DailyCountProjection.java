package com.ltss.features.analytics.repository;

import java.time.LocalDate;

public interface DailyCountProjection {
    LocalDate getDay();
    long getValue();
}
