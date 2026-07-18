package com.ltss.repository.content;

public interface BusinessCategoryStatisticsProjection {
    Long getCategoryId();
    String getCategoryName();
    String getCategorySlug();
    Long getTotalBusinesses();
    Long getActiveBusinesses();
    Long getPendingBusinesses();
    Long getInactiveOrSuspendedBusinesses();
}
