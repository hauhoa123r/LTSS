package com.ltss.features.place.controller;

import com.ltss.common.response.ApiResponse;
import com.ltss.common.response.ApiResponseFactory;
import com.ltss.common.response.PageResponse;
import com.ltss.features.place.dto.PlaceSummaryResponse;
import com.ltss.features.place.dto.SearchHistoryResponse;
import com.ltss.features.place.service.FavoriteService;
import com.ltss.features.place.service.SearchHistoryService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/account")
public class PlaceAccountController {
    private final FavoriteService favoriteService;
    private final SearchHistoryService searchHistoryService;
    private final ApiResponseFactory responseFactory;

    public PlaceAccountController(
            FavoriteService favoriteService,
            SearchHistoryService searchHistoryService,
            ApiResponseFactory responseFactory
    ) {
        this.favoriteService = favoriteService;
        this.searchHistoryService = searchHistoryService;
        this.responseFactory = responseFactory;
    }

    @GetMapping("/favorites")
    public ApiResponse<PageResponse<PlaceSummaryResponse>> favorites(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(50) int size
    ) {
        return responseFactory.success(favoriteService.listMine(page, size));
    }

    @GetMapping("/search-history")
    public ApiResponse<List<SearchHistoryResponse>> searchHistory() {
        return responseFactory.success(searchHistoryService.listMine());
    }

    @DeleteMapping("/search-history")
    public ApiResponse<Void> clearSearchHistory() {
        searchHistoryService.clearMine();
        return responseFactory.success(null);
    }

    @DeleteMapping("/search-history/{historyId}")
    public ApiResponse<Void> deleteSearchHistory(@PathVariable @Min(1) Long historyId) {
        searchHistoryService.deleteMine(historyId);
        return responseFactory.success(null);
    }
}
