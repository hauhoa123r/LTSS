package com.ltss.features.content.controller;

import com.ltss.common.response.ApiResponse;
import com.ltss.common.response.ApiResponseFactory;
import com.ltss.common.response.PageResponse;
import com.ltss.features.content.dto.PromotionResponse;
import com.ltss.features.content.service.BusinessPublicService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/promotions")
public class PromotionController {
    private final BusinessPublicService service;
    private final ApiResponseFactory responseFactory;

    public PromotionController(BusinessPublicService service, ApiResponseFactory responseFactory) {
        this.service = service;
        this.responseFactory = responseFactory;
    }

    @GetMapping
    public ApiResponse<PageResponse<PromotionResponse>> list(
            @RequestParam(required = false) @Min(1) Long businessId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(50) int size
    ) {
        return responseFactory.success(service.promotions(businessId, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<PromotionResponse> detail(@PathVariable @Min(1) Long id) {
        return responseFactory.success(service.promotion(id));
    }
}
