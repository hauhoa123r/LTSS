package com.ltss.features.place.dto;

import com.ltss.features.place.repository.HotspotProjection;

import java.math.BigDecimal;

public record HotspotResponse(
        Long id,
        Long targetMediaId,
        String type,
        BigDecimal yawDegrees,
        BigDecimal pitchDegrees,
        String label,
        String description,
        Integer displayOrder
) {
    public static HotspotResponse from(HotspotProjection hotspot) {
        return new HotspotResponse(
                hotspot.getId(),
                hotspot.getTargetMediaAssetId(),
                hotspot.getHotspotType(),
                hotspot.getYawDegrees(),
                hotspot.getPitchDegrees(),
                hotspot.getLabel(),
                hotspot.getDescription(),
                hotspot.getDisplayOrder()
        );
    }
}
