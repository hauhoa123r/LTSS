package com.ltss.common.exception;

import org.springframework.http.HttpStatus;

public class MonumentStatisticsUnavailableException extends ApplicationException {
    public MonumentStatisticsUnavailableException() {
        super("MONUMENT_STATISTICS_UNAVAILABLE", HttpStatus.INTERNAL_SERVER_ERROR,
                "Không thể tải thống kê lượt xem di tích.");
    }
}
