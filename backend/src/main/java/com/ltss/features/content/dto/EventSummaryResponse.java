package com.ltss.features.content.dto;

import java.time.Instant;

public record EventSummaryResponse(
        Long id,
        String title,
        String slug,
        Instant startAt,
        Instant endAt,
        String locationNote,
        LinkedPlaceResponse place,
        String coverUrl
) {
}
