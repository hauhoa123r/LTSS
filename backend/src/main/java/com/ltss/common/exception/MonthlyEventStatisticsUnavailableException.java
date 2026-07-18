package com.ltss.common.exception;

import org.springframework.http.HttpStatus;

public class MonthlyEventStatisticsUnavailableException extends ApplicationException {
    public MonthlyEventStatisticsUnavailableException() {
        super("MONTHLY_EVENT_STATISTICS_UNAVAILABLE", HttpStatus.INTERNAL_SERVER_ERROR,
                "Không thể tải thống kê sự kiện theo tháng.");
    }
}
