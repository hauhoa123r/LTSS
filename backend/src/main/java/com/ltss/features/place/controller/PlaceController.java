package com.ltss.features.place.controller;

import com.ltss.common.response.ApiResponse;
import com.ltss.common.response.ApiResponseFactory;
import com.ltss.common.response.PageResponse;
import com.ltss.features.place.dto.FavoriteStateResponse;
import com.ltss.features.place.dto.PlaceDetailResponse;
import com.ltss.features.place.dto.PlaceSummaryResponse;
import com.ltss.features.place.service.DiscoveryService;
import com.ltss.features.place.service.FavoriteService;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/places")
public class PlaceController {
    private final DiscoveryService discoveryService;
    private final FavoriteService favoriteService;
    private final ApiResponseFactory responseFactory;

    public PlaceController(
            DiscoveryService discoveryService,
            FavoriteService favoriteService,
            ApiResponseFactory responseFactory
    ) {
        this.discoveryService = discoveryService;
        this.favoriteService = favoriteService;
        this.responseFactory = responseFactory;
    }

    @GetMapping
    public ApiResponse<PageResponse<PlaceSummaryResponse>> search(
            @RequestParam(required = false) @Size(max = 255) String q,
            @RequestParam(required = false) @Size(max = 120) String category,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(50) int size
    ) {
        return responseFactory.success(discoveryService.search(q, category, page, size));
    }

    @GetMapping("/nearby")
    public ApiResponse<PageResponse<PlaceSummaryResponse>> nearby(
            @RequestParam @DecimalMin("-90.0") @DecimalMax("90.0") double latitude,
            @RequestParam @DecimalMin("-180.0") @DecimalMax("180.0") double longitude,
            @RequestParam(defaultValue = "5") @DecimalMin("0.1") @DecimalMax("5.0") double radiusKm,
            @RequestParam(required = false) @Size(max = 120) String category,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(50) int size
    ) {
        return responseFactory.success(discoveryService.nearby(
                latitude, longitude, radiusKm, category, page, size
        ));
    }

    @GetMapping("/{slug}")
    public ApiResponse<PlaceDetailResponse> detail(
            @PathVariable @Size(min = 1, max = 220) String slug
    ) {
        return responseFactory.success(discoveryService.detail(slug));
    }

    @PostMapping("/{placeId}/favorite")
    public ApiResponse<FavoriteStateResponse> favorite(@PathVariable @Min(1) Long placeId) {
        return responseFactory.success(favoriteService.add(placeId));
    }

    @DeleteMapping("/{placeId}/favorite")
    public ApiResponse<FavoriteStateResponse> unfavorite(@PathVariable @Min(1) Long placeId) {
        return responseFactory.success(favoriteService.remove(placeId));
    }
}
