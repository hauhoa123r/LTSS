package com.ltss.dto.analytics;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record MonthlyEventStatisticsResponse(
        int year,
        int month,
        LocalDate startDate,
        LocalDate endDate,
        Instant generatedAt,
        long totalEvents,
        long historicalEvents,
        long activeEvents,
        long upcomingEvents,
        long participantRegistrations,
        long authenticatedParticipants,
        String highestAttendedEvent,
        long highestAttendedRegistrations,
        List<MetricCountResponse> periodBreakdown,
        List<DailyCountResponse> dailyRegistrations,
        List<MonthlyEventSummaryResponse> events
) {}
