package com.ltss.controller.tour;

import com.ltss.common.response.*;
import com.ltss.controller.auth.ClientRequestInfoFactory;
import com.ltss.dto.auth.response.MessageResponse;
import com.ltss.dto.tour.*;
import com.ltss.service.tour.TourService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
public class TourController {
    private final TourService service;
    private final ApiResponseFactory responseFactory;
    private final ClientRequestInfoFactory requestInfoFactory;

    public TourController(TourService service, ApiResponseFactory responseFactory,
                          ClientRequestInfoFactory requestInfoFactory) {
        this.service = service;
        this.responseFactory = responseFactory;
        this.requestInfoFactory = requestInfoFactory;
    }

    @GetMapping("/api/v1/tours")
    public ApiResponse<PageResponse<TourSummaryResponse>> publicTours(
            @RequestParam(required = false) @Size(max = 200) String q,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(50) int size) {
        return responseFactory.success(service.publicTours(q, page, size));
    }

    @GetMapping("/api/v1/tours/{tourId}")
    public ApiResponse<TourDetailResponse> detail(@PathVariable @Min(1) Long tourId) {
        return responseFactory.success(service.detail(tourId));
    }

    @PostMapping("/api/v1/tours/{tourId}/copy")
    public ApiResponse<TourDetailResponse> copy(@PathVariable @Min(1) Long tourId,
                                                HttpServletRequest httpRequest) {
        return responseFactory.success(service.copy(tourId, requestInfoFactory.from(httpRequest)));
    }

    @GetMapping("/api/v1/account/tours")
    public ApiResponse<PageResponse<TourSummaryResponse>> mine(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return responseFactory.success(service.mine(page, size));
    }

    @PostMapping("/api/v1/account/tours")
    public ApiResponse<TourDetailResponse> create(@Valid @RequestBody TourUpsertRequest request,
                                                  HttpServletRequest httpRequest) {
        return responseFactory.success(service.create(request, requestInfoFactory.from(httpRequest)));
    }

    @PutMapping("/api/v1/account/tours/{tourId}")
    public ApiResponse<TourDetailResponse> update(@PathVariable @Min(1) Long tourId,
                                                  @Valid @RequestBody TourUpsertRequest request,
                                                  HttpServletRequest httpRequest) {
        return responseFactory.success(service.update(tourId, request, requestInfoFactory.from(httpRequest)));
    }

    @PutMapping("/api/v1/account/tours/{tourId}/visibility")
    public ApiResponse<TourDetailResponse> changeVisibility(
            @PathVariable @Min(1) Long tourId,
            @Valid @RequestBody ChangeTourVisibilityRequest request,
            HttpServletRequest httpRequest) {
        return responseFactory.success(service.changeVisibility(tourId, request, requestInfoFactory.from(httpRequest)));
    }

    @DeleteMapping("/api/v1/account/tours/{tourId}")
    public ApiResponse<MessageResponse> delete(@PathVariable @Min(1) Long tourId,
                                               @RequestParam @Min(0) Integer version,
                                               HttpServletRequest httpRequest) {
        service.delete(tourId, version, requestInfoFactory.from(httpRequest));
        return responseFactory.success(new MessageResponse("Tour deleted successfully"));
    }
}
