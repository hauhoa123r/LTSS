package com.ltss.controller.content;

import com.ltss.common.response.ApiResponse;
import com.ltss.common.response.ApiResponseFactory;
import com.ltss.common.response.PageResponse;
import com.ltss.dto.content.BusinessPostDetailResponse;
import com.ltss.dto.content.BusinessPostSummaryResponse;
import com.ltss.service.content.BusinessPublicService;
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
@RequestMapping("/api/v1/business-posts")
public class BusinessPostController {
    private final BusinessPublicService service;
    private final ApiResponseFactory responseFactory;

    public BusinessPostController(BusinessPublicService service, ApiResponseFactory responseFactory) {
        this.service = service;
        this.responseFactory = responseFactory;
    }

    @GetMapping
    public ApiResponse<PageResponse<BusinessPostSummaryResponse>> list(
            @RequestParam(required = false) @Size(max = 255) String q,
            @RequestParam(required = false) @Min(1) Long businessId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(50) int size
    ) {
        return responseFactory.success(service.posts(q, businessId, page, size));
    }

    @GetMapping("/{slug}")
    public ApiResponse<BusinessPostDetailResponse> detail(
            @PathVariable @Size(min = 1, max = 280) String slug
    ) {
        return responseFactory.success(service.post(slug));
    }
}
