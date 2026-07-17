package com.ltss.controller.analytics;

import com.ltss.common.response.*;
import com.ltss.dto.analytics.*;
import com.ltss.service.analytics.EngagementService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/engagement-events")
public class EngagementController {
    private final EngagementService service;
    private final ApiResponseFactory responseFactory;

    public EngagementController(EngagementService service, ApiResponseFactory responseFactory) {
        this.service = service;
        this.responseFactory = responseFactory;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EngagementAcceptedResponse>> record(
            @Valid @RequestBody EngagementEventRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(responseFactory.success(service.record(request)));
    }
}
