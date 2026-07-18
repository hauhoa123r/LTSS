package com.ltss.repository.analytics;

import java.time.Instant;

public interface MonumentVisitProjection {
    Long getPlaceId();
    String getName();
    String getSlug();
    String getAddress();
    long getVisits();
    long getUniqueSessions();
    long getAuthenticatedVisitors();
    Instant getLastVisitAt();
}
