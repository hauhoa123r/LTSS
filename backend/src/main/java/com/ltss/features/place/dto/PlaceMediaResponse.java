package com.ltss.features.place.dto;

import java.math.BigDecimal;
import java.util.List;

public record PlaceMediaResponse(
        Long id,
        String mediaType,
        String mediaUrl,
        String thumbnailUrl,
        String mimeType,
        Long fileSizeBytes,
        Integer widthPx,
        Integer heightPx,
        BigDecimal durationSeconds,
        String usageType,
        Integer displayOrder,
        boolean primary,
        List<HotspotResponse> hotspots
) {
}
