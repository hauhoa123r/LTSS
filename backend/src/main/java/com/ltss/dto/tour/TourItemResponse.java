package com.ltss.dto.tour;

import java.time.Instant;

public record TourItemResponse(
        Long id, Long placeId, String placeName, String placeSlug, String address,
        short visitOrder, Instant plannedStartAt, Integer durationMinutes,
        String transportMethod, String note
) {}
