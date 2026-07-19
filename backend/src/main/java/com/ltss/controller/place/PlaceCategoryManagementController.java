package com.ltss.controller.place;

import com.ltss.common.response.ApiResponse;
import com.ltss.common.response.ApiResponseFactory;
import com.ltss.common.response.PageResponse;
import com.ltss.controller.auth.ClientRequestInfoFactory;
import com.ltss.dto.auth.response.MessageResponse;
import com.ltss.dto.place.PlaceCategoryManagementRequest;
import com.ltss.dto.place.PlaceCategoryManagementResponse;
import com.ltss.service.place.PlaceCategoryManagementService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/v1/moderation/place-categories")
public class PlaceCategoryManagementController {
    private final PlaceCategoryManagementService service;
    private final ApiResponseFactory responseFactory;
    private final ClientRequestInfoFactory requestInfoFactory;

    public PlaceCategoryManagementController(
            PlaceCategoryManagementService service,
            ApiResponseFactory responseFactory,
            ClientRequestInfoFactory requestInfoFactory
    ) {
        this.service = service;
        this.responseFactory = responseFactory;
        this.requestInfoFactory = requestInfoFactory;
    }

    @GetMapping
    public ApiResponse<PageResponse<PlaceCategoryManagementResponse>> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "5") @Min(1) @Max(50) int size
    ) {
        return responseFactory.success(service.list(page, size));
    }

    @GetMapping("/{categoryId}")
    public ApiResponse<PlaceCategoryManagementResponse> get(
            @PathVariable @Min(1) Long categoryId
    ) {
        return responseFactory.success(service.get(categoryId));
    }

    @PostMapping
    public ApiResponse<PlaceCategoryManagementResponse> create(
            @Valid @RequestBody PlaceCategoryManagementRequest request,
            HttpServletRequest httpRequest
    ) {
        return responseFactory.success(service.create(request, requestInfoFactory.from(httpRequest)));
    }

    @PutMapping("/{categoryId}")
    public ApiResponse<PlaceCategoryManagementResponse> update(
            @PathVariable @Min(1) Long categoryId,
            @Valid @RequestBody PlaceCategoryManagementRequest request,
            HttpServletRequest httpRequest
    ) {
        return responseFactory.success(service.update(categoryId, request, requestInfoFactory.from(httpRequest)));
    }

    @DeleteMapping("/{categoryId}")
    public ApiResponse<MessageResponse> delete(
            @PathVariable @Min(1) Long categoryId,
            HttpServletRequest httpRequest
    ) {
        service.delete(categoryId, requestInfoFactory.from(httpRequest));
        return responseFactory.success(new MessageResponse("Place category deactivated successfully"));
    }
}
