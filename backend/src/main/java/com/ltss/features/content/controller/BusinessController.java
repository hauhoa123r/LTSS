package com.ltss.features.content.controller;

import com.ltss.common.response.ApiResponse;
import com.ltss.common.response.ApiResponseFactory;
import com.ltss.common.response.PageResponse;
import com.ltss.features.content.dto.BusinessResponse;
import com.ltss.features.content.service.BusinessPublicService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/businesses")
public class BusinessController {
    private final BusinessPublicService service;
    private final ApiResponseFactory responseFactory;

    public BusinessController(BusinessPublicService service, ApiResponseFactory responseFactory) {
        this.service = service;
        this.responseFactory = responseFactory;
    }

    @GetMapping
    public ApiResponse<PageResponse<BusinessResponse>> list(
            @RequestParam(required = false) @Size(max = 255) String q,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(50) int size
    ) {
        return responseFactory.success(service.businesses(q, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<BusinessResponse> detail(@PathVariable @Min(1) Long id) {
        return responseFactory.success(service.business(id));
    }
}
