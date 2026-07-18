package com.ltss.common.exception;

import org.springframework.http.HttpStatus;

public class BusinessStatisticsUnavailableException extends ApplicationException {
    public BusinessStatisticsUnavailableException() {
        super("BUSINESS_STATISTICS_UNAVAILABLE", HttpStatus.INTERNAL_SERVER_ERROR,
                "Không thể tải thống kê tài khoản doanh nghiệp.");
    }
}
