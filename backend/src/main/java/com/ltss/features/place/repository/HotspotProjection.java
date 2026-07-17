package com.ltss.features.place.repository;

import java.math.BigDecimal;

public interface HotspotProjection {
    Long getId();
    Long getSourceMediaAssetId();
    Long getTargetMediaAssetId();
    String getHotspotType();
    BigDecimal getYawDegrees();
    BigDecimal getPitchDegrees();
    String getLabel();
    String getDescription();
    Integer getDisplayOrder();
}
