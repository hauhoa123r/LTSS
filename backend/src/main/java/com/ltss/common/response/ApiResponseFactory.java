package com.ltss.common.response;

import com.ltss.common.logging.RequestIdFilter;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class ApiResponseFactory {

    public <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(
                true,
                "SUCCESS",
                "Request completed successfully",
                data,
                null,
                Instant.now(),
                currentRequestId()
        );
    }

    public ApiResponse<Void> error(String code, String message) {
        return error(code, message, List.of());
    }

    public ApiResponse<Void> error(
            String code,
            String message,
            List<FieldErrorDetail> fieldErrors
    ) {
        ApiError apiError = fieldErrors == null || fieldErrors.isEmpty()
                ? null
                : new ApiError(fieldErrors);

        return new ApiResponse<>(
                false,
                code,
                message,
                null,
                apiError,
                Instant.now(),
                currentRequestId()
        );
    }

    private String currentRequestId() {
        String requestId = MDC.get(RequestIdFilter.MDC_KEY);
        return StringUtils.hasText(requestId) ? requestId : UUID.randomUUID().toString();
    }
}
