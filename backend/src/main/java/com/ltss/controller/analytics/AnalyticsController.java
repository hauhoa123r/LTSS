package com.ltss.controller.analytics;

import com.ltss.common.response.*;
import com.ltss.dto.analytics.*;
import com.ltss.service.analytics.AnalyticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1")
public class AnalyticsController {
    private final AnalyticsService service;
    private final ApiResponseFactory responseFactory;

    public AnalyticsController(AnalyticsService service, ApiResponseFactory responseFactory) {
        this.service = service;
        this.responseFactory = responseFactory;
    }

    @GetMapping("/analytics/system")
    public ApiResponse<AnalyticsOverviewResponse> system(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return responseFactory.success(service.system(from, to));
    }

    @GetMapping("/analytics/business")
    public ApiResponse<AnalyticsOverviewResponse> business(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return responseFactory.success(service.ownBusiness(from, to));
    }

    @GetMapping("/admin/dashboard")
    public ApiResponse<AdminDashboardResponse> dashboard(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return responseFactory.success(service.dashboard(from, to));
    }

    @GetMapping("/admin/monument-statistics")
    public ApiResponse<MonumentStatisticsResponse> monumentStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "DAILY") MonumentGranularity granularity) {
        return responseFactory.success(service.monumentStatistics(startDate, endDate, granularity));
    }

    @GetMapping("/admin/business-statistics")
    public ApiResponse<BusinessStatisticsResponse> businessStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return responseFactory.success(service.businessStatistics(startDate, endDate));
    }

    @GetMapping("/admin/monthly-event-statistics")
    public ApiResponse<MonthlyEventStatisticsResponse> monthlyEventStatistics(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        return responseFactory.success(service.monthlyEventStatistics(year, month));
    }

    @GetMapping("/admin/retention-status")
    public ApiResponse<RetentionStatusResponse> retentionStatus() {
        return responseFactory.success(service.retentionStatus());
    }
}
