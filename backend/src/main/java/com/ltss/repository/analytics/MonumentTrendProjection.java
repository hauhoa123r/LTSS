package com.ltss.repository.analytics;

import java.time.LocalDate;

public interface MonumentTrendProjection {
    Long getPlaceId();
    LocalDate getDay();
    long getValue();
}
