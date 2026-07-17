package com.ltss.dto.content;

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
