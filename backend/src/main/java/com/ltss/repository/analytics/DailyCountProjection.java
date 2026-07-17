package com.ltss.repository.analytics;

import java.time.LocalDate;

public interface DailyCountProjection {
    LocalDate getDay();
    long getValue();
}
