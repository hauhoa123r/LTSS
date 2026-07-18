package com.ltss.dto.analytics;

import java.time.Instant;

public record MonthlyEventSummaryResponse(
        Long eventId,
        String title,
        String slug,
        String placeName,
        String locationNote,
        Instant startAt,
        Instant endAt,
        String periodStatus,
        long participantRegistrations,
        long authenticatedParticipants,
        long engagementCount
) {}
