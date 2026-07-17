package com.ltss.features.community.controller;

import com.ltss.common.response.*;
import com.ltss.features.auth.controller.ClientRequestInfoFactory;
import com.ltss.features.community.dto.*;
import com.ltss.features.community.entity.ReviewTargetType;
import com.ltss.features.community.service.ReviewService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
public class ReviewController {
    private final ReviewService service;
    private final ApiResponseFactory responseFactory;
    private final ClientRequestInfoFactory requestInfoFactory;

    public ReviewController(ReviewService service, ApiResponseFactory responseFactory,
                            ClientRequestInfoFactory requestInfoFactory) {
        this.service = service;
        this.responseFactory = responseFactory;
        this.requestInfoFactory = requestInfoFactory;
    }

    @GetMapping("/api/v1/reviews")
    public ApiResponse<PageResponse<ReviewResponse>> visible(
            @RequestParam ReviewTargetType targetType,
            @RequestParam @Min(1) Long targetId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        return responseFactory.success(service.visible(targetType, targetId, page, size));
    }

    @PostMapping("/api/v1/reviews/{targetType}/{targetId}")
    public ApiResponse<ReviewResponse> create(@PathVariable ReviewTargetType targetType,
                                              @PathVariable @Min(1) Long targetId,
                                              @Valid @RequestBody CreateReviewRequest request,
                                              HttpServletRequest httpRequest) {
        return responseFactory.success(service.create(targetType, targetId, request,
                requestInfoFactory.from(httpRequest)));
    }

    @GetMapping("/api/v1/account/reviews")
    public ApiResponse<PageResponse<ReviewResponse>> mine(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return responseFactory.success(service.mine(page, size));
    }

    @PostMapping("/api/v1/reviews/{reviewId}/reply")
    public ApiResponse<ReviewResponse> reply(@PathVariable @Min(1) Long reviewId,
                                             @Valid @RequestBody ReviewReplyRequest request,
                                             HttpServletRequest httpRequest) {
        return responseFactory.success(service.reply(reviewId, request, requestInfoFactory.from(httpRequest)));
    }
}
