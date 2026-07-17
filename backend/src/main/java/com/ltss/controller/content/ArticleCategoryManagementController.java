package com.ltss.controller.content;

import com.ltss.common.response.ApiResponse;
import com.ltss.common.response.ApiResponseFactory;
import com.ltss.common.response.PageResponse;
import com.ltss.controller.auth.ClientRequestInfoFactory;
import com.ltss.dto.auth.response.MessageResponse;
import com.ltss.dto.content.ArticleCategoryManagementRequest;
import com.ltss.dto.content.ArticleCategoryManagementResponse;
import com.ltss.service.content.ArticleCategoryManagementService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/v1/moderation/article-categories")
public class ArticleCategoryManagementController {
    private final ArticleCategoryManagementService service;
    private final ApiResponseFactory responseFactory;
    private final ClientRequestInfoFactory requestInfoFactory;

    public ArticleCategoryManagementController(
            ArticleCategoryManagementService service,
            ApiResponseFactory responseFactory,
            ClientRequestInfoFactory requestInfoFactory
    ) {
        this.service = service;
        this.responseFactory = responseFactory;
        this.requestInfoFactory = requestInfoFactory;
    }

    @GetMapping
    public ApiResponse<PageResponse<ArticleCategoryManagementResponse>> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "5") @Min(1) @Max(50) int size
    ) {
        return responseFactory.success(service.list(page, size));
    }

    @PostMapping
    public ApiResponse<ArticleCategoryManagementResponse> create(
            @Valid @RequestBody ArticleCategoryManagementRequest request,
            HttpServletRequest httpRequest
    ) {
        return responseFactory.success(service.create(request, requestInfoFactory.from(httpRequest)));
    }

    @PutMapping("/{categoryId}")
    public ApiResponse<ArticleCategoryManagementResponse> update(
            @PathVariable @Min(1) Long categoryId,
            @Valid @RequestBody ArticleCategoryManagementRequest request,
            HttpServletRequest httpRequest
    ) {
        return responseFactory.success(service.update(categoryId, request, requestInfoFactory.from(httpRequest)));
    }

    @DeleteMapping("/{categoryId}")
    public ApiResponse<MessageResponse> delete(
            @PathVariable @Min(1) Long categoryId,
            HttpServletRequest httpRequest
    ) {
        service.delete(categoryId, requestInfoFactory.from(httpRequest));
        return responseFactory.success(new MessageResponse("Article category deactivated successfully"));
    }
}
