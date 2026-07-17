package com.ltss.repository.place;

import java.math.BigDecimal;

public interface NearbyPlaceProjection {
    Long getPlaceId();
    Long getCategoryId();
    String getName();
    String getSlug();
    String getSummary();
    String getAddress();
    BigDecimal getLatitude();
    BigDecimal getLongitude();
    BigDecimal getEntranceFee();
    Double getDistanceKm();
}
