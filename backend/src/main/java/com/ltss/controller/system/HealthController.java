package com.ltss.controller.system;

import com.ltss.common.response.ApiResponse;
import com.ltss.common.response.ApiResponseFactory;
import com.ltss.dto.system.HealthResponse;
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
