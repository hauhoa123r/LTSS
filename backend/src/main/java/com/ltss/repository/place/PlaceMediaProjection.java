package com.ltss.repository.place;

import java.math.BigDecimal;

public interface PlaceMediaProjection {
    Long getPlaceId();
    Long getMediaId();
    String getMediaType();
    String getMediaUrl();
    String getThumbnailUrl();
    String getMimeType();
    Long getFileSizeBytes();
    Integer getWidthPx();
    Integer getHeightPx();
    BigDecimal getDurationSeconds();
    String getUsageType();
    Integer getDisplayOrder();
    Boolean getPrimaryMedia();
}
