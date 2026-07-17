package com.ltss.controller.administration;

import com.ltss.common.response.*;
import com.ltss.dto.administration.*;
import com.ltss.service.administration.*;
import com.ltss.controller.auth.ClientRequestInfoFactory;
import com.ltss.entity.auth.UserStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Validated
@RestController
@RequestMapping("/api/v1/admin")
public class AdministrationController {
    private final AdministrationService administrationService;
    private final AuditQueryService auditQueryService;
    private final ApiResponseFactory responseFactory;
    private final ClientRequestInfoFactory requestInfoFactory;

    public AdministrationController(AdministrationService administrationService,
                                    AuditQueryService auditQueryService,
                                    ApiResponseFactory responseFactory,
                                    ClientRequestInfoFactory requestInfoFactory) {
        this.administrationService = administrationService;
        this.auditQueryService = auditQueryService;
        this.responseFactory = responseFactory;
        this.requestInfoFactory = requestInfoFactory;
    }

    @GetMapping("/users")
    public ApiResponse<PageResponse<AdminUserResponse>> users(
            @RequestParam(required = false) @Size(max = 200) String q,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return responseFactory.success(administrationService.users(q, status, page, size));
    }

    @GetMapping("/users/{userId}")
    public ApiResponse<AdminUserResponse> user(@PathVariable @Min(1) Long userId) {
        return responseFactory.success(administrationService.user(userId));
    }

    @PutMapping("/users/{userId}/status")
    public ApiResponse<AdminUserResponse> changeStatus(
            @PathVariable @Min(1) Long userId,
            @Valid @RequestBody ChangeUserStatusRequest request,
            HttpServletRequest httpRequest) {
        return responseFactory.success(administrationService.changeStatus(
                userId, request, requestInfoFactory.from(httpRequest)));
    }

    @PutMapping("/users/{userId}/roles/{roleCode}")
    public ApiResponse<AdminUserResponse> assignRole(
            @PathVariable @Min(1) Long userId,
            @PathVariable @Pattern(regexp = "[A-Z][A-Z0-9_]{0,29}") String roleCode,
            @Valid @RequestBody RoleChangeRequest request,
            HttpServletRequest httpRequest) {
        return responseFactory.success(administrationService.assignRole(
                userId, roleCode, request, requestInfoFactory.from(httpRequest)));
    }

    @DeleteMapping("/users/{userId}/roles/{roleCode}")
    public ApiResponse<AdminUserResponse> revokeRole(
            @PathVariable @Min(1) Long userId,
            @PathVariable @Pattern(regexp = "[A-Z][A-Z0-9_]{0,29}") String roleCode,
            @Valid @RequestBody RoleChangeRequest request,
            HttpServletRequest httpRequest) {
        return responseFactory.success(administrationService.revokeRole(
                userId, roleCode, request, requestInfoFactory.from(httpRequest)));
    }

    @GetMapping("/audit-logs")
    public ApiResponse<PageResponse<AuditLogResponse>> auditLogs(
            @RequestParam(required = false) @Min(1) Long actorId,
            @RequestParam(required = false) @Size(max = 100) String action,
            @RequestParam(required = false) @Size(max = 100) String entityType,
            @RequestParam(required = false) @Min(1) Long entityId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        return responseFactory.success(auditQueryService.search(
                actorId, action, entityType, entityId, from, to, page, size));
    }

    @GetMapping("/audit-metadata")
    public ApiResponse<AuditMetadataResponse> auditMetadata() {
        return responseFactory.success(auditQueryService.metadata());
    }

    @GetMapping("/audit-logs/{auditLogId}")
    public ApiResponse<AuditLogResponse> auditLog(@PathVariable @Min(1) Long auditLogId) {
        return responseFactory.success(auditQueryService.detail(auditLogId));
    }
}
