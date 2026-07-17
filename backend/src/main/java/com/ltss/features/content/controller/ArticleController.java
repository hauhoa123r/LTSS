package com.ltss.features.content.controller;

import com.ltss.common.response.ApiResponse;
import com.ltss.common.response.ApiResponseFactory;
import com.ltss.common.response.PageResponse;
import com.ltss.features.content.dto.ArticleCategoryResponse;
import com.ltss.features.content.dto.ArticleDetailResponse;
import com.ltss.features.content.dto.ArticleSummaryResponse;
import com.ltss.features.content.service.EditorialPublicService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
public class ArticleController {
    private final EditorialPublicService service;
    private final ApiResponseFactory responseFactory;

    public ArticleController(EditorialPublicService service, ApiResponseFactory responseFactory) {
        this.service = service;
        this.responseFactory = responseFactory;
    }

    @GetMapping("/api/v1/article-categories")
    public ApiResponse<List<ArticleCategoryResponse>> categories() {
        return responseFactory.success(service.categories());
    }

    @GetMapping("/api/v1/articles")
    public ApiResponse<PageResponse<ArticleSummaryResponse>> list(
            @RequestParam(required = false) @Size(max = 255) String q,
            @RequestParam(required = false) @Size(max = 120) String category,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(50) int size
    ) {
        return responseFactory.success(service.articles(q, category, page, size));
    }

    @GetMapping("/api/v1/articles/{slug}")
    public ApiResponse<ArticleDetailResponse> detail(
            @PathVariable @Size(min = 1, max = 280) String slug
    ) {
        return responseFactory.success(service.article(slug));
    }
}
