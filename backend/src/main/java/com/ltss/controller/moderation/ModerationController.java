package com.ltss.controller.moderation;

import com.ltss.common.response.ApiResponse;
import com.ltss.common.response.ApiResponseFactory;
import com.ltss.common.response.PageResponse;
import com.ltss.controller.auth.ClientRequestInfoFactory;
import com.ltss.dto.moderation.*;
import com.ltss.entity.moderation.ModerationTargetType;
import com.ltss.service.moderation.ModerationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/v1/moderation")
public class ModerationController {
    private final ModerationService service;
    private final ApiResponseFactory responseFactory;
    private final ClientRequestInfoFactory requestInfoFactory;

    public ModerationController(
            ModerationService service,
            ApiResponseFactory responseFactory,
            ClientRequestInfoFactory requestInfoFactory
    ) {
        this.service = service;
        this.responseFactory = responseFactory;
        this.requestInfoFactory = requestInfoFactory;
    }

    @PostMapping("/targets/{targetType}/{targetId}/submit")
    public ApiResponse<ModerationRecordResponse> submit(
            @PathVariable ModerationTargetType targetType,
            @PathVariable @Min(1) Long targetId,
            @Valid @RequestBody SubmitModerationRequest request,
            HttpServletRequest httpRequest
    ) {
        return responseFactory.success(service.submit(
                targetType, targetId, request, requestInfoFactory.from(httpRequest)
        ));
    }

    @GetMapping("/queue")
    public ApiResponse<PageResponse<ModerationRecordResponse>> queue(
            @RequestParam(required = false) ModerationTargetType targetType,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size
    ) {
        return responseFactory.success(service.queue(targetType, page, size));
    }

    @GetMapping("/{caseId}")
    public ApiResponse<ModerationRecordResponse> detail(@PathVariable @Min(1) Long caseId) {
        return responseFactory.success(service.detail(caseId));
    }

    @GetMapping("/targets/{targetType}/{targetId}/history")
    public ApiResponse<PageResponse<ModerationRecordResponse>> history(
            @PathVariable ModerationTargetType targetType,
            @PathVariable @Min(1) Long targetId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size
    ) {
        return responseFactory.success(service.history(targetType, targetId, page, size));
    }

    @PostMapping("/{caseId}/approve")
    public ApiResponse<ModerationRecordResponse> approve(
            @PathVariable @Min(1) Long caseId,
            @Valid @RequestBody ModerationDecisionRequest request,
            HttpServletRequest httpRequest
    ) {
        return responseFactory.success(service.approve(caseId, request, requestInfoFactory.from(httpRequest)));
    }

    @PostMapping("/{caseId}/reject")
    public ApiResponse<ModerationRecordResponse> reject(
            @PathVariable @Min(1) Long caseId,
            @Valid @RequestBody ModerationDecisionRequest request,
            HttpServletRequest httpRequest
    ) {
        return responseFactory.success(service.reject(caseId, request, requestInfoFactory.from(httpRequest)));
    }

    @PostMapping("/{caseId}/cancel")
    public ApiResponse<ModerationRecordResponse> cancel(
            @PathVariable @Min(1) Long caseId,
            @Valid @RequestBody CancelModerationRequest request,
            HttpServletRequest httpRequest
    ) {
        return responseFactory.success(service.cancel(caseId, request, requestInfoFactory.from(httpRequest)));
    }
}
