package com.ltss.repository.content;

import java.time.Instant;

public interface MonthlyEventStatisticsProjection {
    Long getEventId();
    String getTitle();
    String getSlug();
    String getPlaceName();
    String getLocationNote();
    Instant getStartAt();
    Instant getEndAt();
    String getPeriodStatus();
    Long getParticipantRegistrations();
    Long getAuthenticatedParticipants();
    Long getEngagementCount();
}
