package com.ltss.features.moderation.controller;

import com.ltss.common.response.ApiResponse;
import com.ltss.common.response.ApiResponseFactory;
import com.ltss.common.response.PageResponse;
import com.ltss.features.moderation.dto.NotificationResponse;
import com.ltss.features.moderation.dto.UnreadNotificationCountResponse;
import com.ltss.features.moderation.service.NotificationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/v1/account/notifications")
public class NotificationController {
    private final NotificationService service;
    private final ApiResponseFactory responseFactory;

    public NotificationController(NotificationService service, ApiResponseFactory responseFactory) {
        this.service = service;
        this.responseFactory = responseFactory;
    }

    @GetMapping
    public ApiResponse<PageResponse<NotificationResponse>> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size
    ) {
        return responseFactory.success(service.list(page, size));
    }

    @GetMapping("/unread-count")
    public ApiResponse<UnreadNotificationCountResponse> unreadCount() {
        return responseFactory.success(service.unreadCount());
    }

    @PostMapping("/{notificationId}/read")
    public ApiResponse<NotificationResponse> markRead(@PathVariable @Min(1) Long notificationId) {
        return responseFactory.success(service.markRead(notificationId));
    }
}
