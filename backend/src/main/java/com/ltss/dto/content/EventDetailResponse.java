package com.ltss.dto.content;

import java.time.Instant;
import java.util.List;

public record EventDetailResponse(
        Long id,
        String title,
        String slug,
        String description,
        Instant startAt,
        Instant endAt,
        String locationNote,
        LinkedPlaceResponse place,
        List<ContentMediaResponse> media,
        Integer version
) {
}
