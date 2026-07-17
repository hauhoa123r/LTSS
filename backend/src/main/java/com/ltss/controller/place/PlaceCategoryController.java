package com.ltss.controller.place;

import com.ltss.common.response.ApiResponse;
import com.ltss.common.response.ApiResponseFactory;
import com.ltss.dto.place.PlaceCategoryResponse;
import com.ltss.service.place.DiscoveryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/place-categories")
public class PlaceCategoryController {
    private final DiscoveryService discoveryService;
    private final ApiResponseFactory responseFactory;

    public PlaceCategoryController(DiscoveryService discoveryService, ApiResponseFactory responseFactory) {
        this.discoveryService = discoveryService;
        this.responseFactory = responseFactory;
    }

    @GetMapping
    public ApiResponse<List<PlaceCategoryResponse>> list() {
        return responseFactory.success(discoveryService.categories());
    }
}
