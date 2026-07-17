package com.ltss.features.system.controller;

import com.ltss.common.response.ApiResponse;
import com.ltss.common.response.ApiResponseFactory;
import com.ltss.features.system.dto.HealthResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
public class HealthController {

    private final ApiResponseFactory responseFactory;

    @GetMapping
    public ApiResponse<HealthResponse> health() {
        return responseFactory.success(new HealthResponse("UP", "ltss-backend"));
    }
}
