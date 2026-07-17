package com.ltss.controller.content;

import com.ltss.common.response.ApiResponse;
import com.ltss.common.response.ApiResponseFactory;
import com.ltss.common.response.PageResponse;
import com.ltss.controller.auth.ClientRequestInfoFactory;
import com.ltss.dto.auth.response.MessageResponse;
import com.ltss.dto.content.RelicArticleResponse;
import com.ltss.dto.content.RelicArticleUpsertRequest;
import com.ltss.service.content.RelicArticleService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/v1/management/articles")
public class RelicArticleController {
    private final RelicArticleService service;
    private final ApiResponseFactory responseFactory;
    private final ClientRequestInfoFactory requestInfoFactory;

    public RelicArticleController(
            RelicArticleService service,
            ApiResponseFactory responseFactory,
            ClientRequestInfoFactory requestInfoFactory
    ) {
        this.service = service;
        this.responseFactory = responseFactory;
        this.requestInfoFactory = requestInfoFactory;
    }

    @GetMapping
    public ApiResponse<PageResponse<RelicArticleResponse>> mine(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size
    ) {
        return responseFactory.success(service.mine(page, size));
    }

    @GetMapping("/{articleId}")
    public ApiResponse<RelicArticleResponse> detail(@PathVariable @Min(1) Long articleId) {
        return responseFactory.success(service.detail(articleId));
    }

    @PostMapping
    public ApiResponse<RelicArticleResponse> create(
            @Valid @RequestBody RelicArticleUpsertRequest request,
            HttpServletRequest httpRequest
    ) {
        return responseFactory.success(service.create(request, requestInfoFactory.from(httpRequest)));
    }

    @PutMapping("/{articleId}")
    public ApiResponse<RelicArticleResponse> update(
            @PathVariable @Min(1) Long articleId,
            @Valid @RequestBody RelicArticleUpsertRequest request,
            HttpServletRequest httpRequest
    ) {
        return responseFactory.success(service.update(articleId, request, requestInfoFactory.from(httpRequest)));
    }

    @DeleteMapping("/{articleId}")
    public ApiResponse<MessageResponse> delete(
            @PathVariable @Min(1) Long articleId,
            @RequestParam @Min(0) Integer version,
            HttpServletRequest httpRequest
    ) {
        service.delete(articleId, version, requestInfoFactory.from(httpRequest));
        return responseFactory.success(new MessageResponse("Article deleted successfully"));
    }
}
