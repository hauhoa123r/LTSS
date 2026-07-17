package com.ltss.controller.content;

import com.ltss.common.response.ApiResponse;
import com.ltss.common.response.ApiResponseFactory;
import com.ltss.common.response.PageResponse;
import com.ltss.dto.content.BusinessOwnerPostResponse;
import com.ltss.dto.content.BusinessOwnerProfileResponse;
import com.ltss.dto.content.BusinessOwnerPromotionResponse;
import com.ltss.service.content.BusinessOwnerWorkspaceService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/v1/business-owner")
public class BusinessOwnerWorkspaceController {
    private final BusinessOwnerWorkspaceService service;
    private final ApiResponseFactory responseFactory;

    public BusinessOwnerWorkspaceController(
            BusinessOwnerWorkspaceService service, ApiResponseFactory responseFactory
    ) {
        this.service = service;
        this.responseFactory = responseFactory;
    }

    @GetMapping("/profile")
    public ApiResponse<BusinessOwnerProfileResponse> profile() {
        return responseFactory.success(service.profile());
    }

    @GetMapping("/posts")
    public ApiResponse<PageResponse<BusinessOwnerPostResponse>> posts(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "5") @Min(1) @Max(50) int size
    ) {
        return responseFactory.success(service.posts(page, size));
    }

    @GetMapping("/promotions")
    public ApiResponse<PageResponse<BusinessOwnerPromotionResponse>> promotions(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "5") @Min(1) @Max(50) int size
    ) {
        return responseFactory.success(service.promotions(page, size));
    }
}
