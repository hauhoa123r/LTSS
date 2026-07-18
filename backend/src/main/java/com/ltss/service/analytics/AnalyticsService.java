package com.ltss.service.analytics;

import com.ltss.dto.analytics.*;
import com.ltss.repository.analytics.*;
import com.ltss.repository.auth.*;
import com.ltss.entity.content.*;
import java.time.*;
import java.util.*;

public interface AnalyticsService {

    AnalyticsOverviewResponse system(LocalDate from, LocalDate to);

    AnalyticsOverviewResponse ownBusiness(LocalDate from, LocalDate to);

    AdminDashboardResponse dashboard(LocalDate from, LocalDate to);

    MonumentStatisticsResponse monumentStatistics(LocalDate startDate, LocalDate endDate,
                                                  MonumentGranularity granularity);

    BusinessStatisticsResponse businessStatistics(LocalDate startDate, LocalDate endDate);

    MonthlyEventStatisticsResponse monthlyEventStatistics(Integer year, Integer month);

    RetentionStatusResponse retentionStatus();
}
