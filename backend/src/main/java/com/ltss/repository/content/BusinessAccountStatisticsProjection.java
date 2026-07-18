package com.ltss.repository.content;

import java.time.Instant;

public interface BusinessAccountStatisticsProjection {
    Long getBusinessId();
    String getBusinessName();
    String getPlaceSlug();
    String getCategoryName();
    String getCategorySlug();
    String getStatus();
    Instant getCreatedAt();
    Instant getApprovedAt();
}
