package com.ltss.controller.content;

import com.ltss.common.response.ApiResponse;
import com.ltss.common.response.ApiResponseFactory;
import com.ltss.common.response.PageResponse;
import com.ltss.dto.content.EventDetailResponse;
import com.ltss.dto.content.EventSummaryResponse;
import com.ltss.service.content.EditorialPublicService;
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
@RequestMapping("/api/v1/events")
public class EventController {
    private final EditorialPublicService service;
    private final ApiResponseFactory responseFactory;

    public EventController(EditorialPublicService service, ApiResponseFactory responseFactory) {
        this.service = service;
        this.responseFactory = responseFactory;
    }

    @GetMapping
    public ApiResponse<PageResponse<EventSummaryResponse>> list(
            @RequestParam(required = false) @Size(max = 255) String q,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(50) int size
    ) {
        return responseFactory.success(service.events(q, page, size));
    }

    @GetMapping("/{slug}")
    public ApiResponse<EventDetailResponse> detail(
            @PathVariable @Size(min = 1, max = 280) String slug
    ) {
        return responseFactory.success(service.event(slug));
    }
}
