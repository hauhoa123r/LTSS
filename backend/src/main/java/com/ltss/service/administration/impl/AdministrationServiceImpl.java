package com.ltss.service.administration.impl;

import com.ltss.service.administration.AdministrationService;

import com.ltss.common.exception.*;
import com.ltss.common.response.PageResponse;
import com.ltss.dto.administration.*;
import com.ltss.entity.auth.*;
import com.ltss.repository.auth.*;
import com.ltss.security.auth.CurrentUserService;
import com.ltss.service.auth.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.*;

@Service
public class AdministrationServiceImpl implements AdministrationService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final AuthorizationRepository authorizationRepository;
    private final CurrentUserService currentUserService;
    private final AccountTokenService tokenService;
    private final AuditService auditService;
    private final Clock clock;

    public AdministrationServiceImpl(UserRepository userRepository, RoleRepository roleRepository,
                                 UserRoleRepository userRoleRepository, AuthorizationRepository authorizationRepository,
                                 CurrentUserService currentUserService, AccountTokenService tokenService,
                                 AuditService auditService, Clock clock) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.authorizationRepository = authorizationRepository;
        this.currentUserService = currentUserService;
        this.tokenService = tokenService;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    @Override
    public PageResponse<AdminUserResponse> users(String query, UserStatus status, int page, int size) {
        requireAdministrator();
        String normalized = query == null || query.isBlank() ? null : query.trim();
        return PageResponse.from(userRepository.searchAdmin(normalized, status, PageRequest.of(page, size)).map(this::response));
    }

    @Transactional(readOnly = true)
    @Override
    public AdminUserResponse user(Long userId) {
        requireAdministrator();
        return response(requireUser(userId));
    }

    @Transactional
    @Override
    public AdminUserResponse changeStatus(Long userId, ChangeUserStatusRequest request,
                                          ClientRequestInfo requestInfo) {
        Long actorId = requireAdministrator();
        requireNotSelf(actorId, userId);
        UserEntity user = userRepository.findLockedById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User was not found"));
        if (!Objects.equals(user.getVersion(), request.version())) {
            throw new ConflictException("User was changed by another request; reload and try again");
        }
        UserStatus old = user.getStatus();
        requireStatusTransition(old, request.status());
        user.changeAdministrativeStatus(request.status(), actorId, clock.instant());
        tokenService.revokeAllRefreshTokens(userId);
        auditService.recordDomainChange(actorId, "ADMIN_USER_STATUS_CHANGED", "USER", userId,
                Map.of("status", old.name()),
                Map.of("status", request.status().name(), "reason", request.reason().trim()), requestInfo);
        userRepository.flush();
        return response(user);
    }

    @Transactional
    @Override
    public AdminUserResponse assignRole(Long userId, String roleCode, RoleChangeRequest request,
                                        ClientRequestInfo requestInfo) {
        Long actorId = requireAdministrator();
        requireNotSelf(actorId, userId);
        requireMutableUser(userId);
        RoleEntity role = roleRepository.findByRoleCodeAndActiveTrue(roleCode)
                .orElseThrow(() -> new ResourceNotFoundException("Active role was not found"));
        UserRoleId id = new UserRoleId(userId, role.getId());
        UserRoleEntity mapping = userRoleRepository.findById(id).orElse(null);
        if (mapping != null && mapping.isActive()) throw new ConflictException("User already has this direct role");
        if (mapping == null) userRoleRepository.save(new UserRoleEntity(userId, role.getId(), actorId));
        else mapping.reactivate(actorId, clock.instant());
        tokenService.revokeAllRefreshTokens(userId);
        auditService.recordDomainChange(actorId, "ADMIN_USER_ROLE_ASSIGNED", "USER", userId,
                Map.of(), Map.of("role", roleCode, "reason", request.reason().trim()), requestInfo);
        return response(requireUser(userId));
    }

    @Transactional
    @Override
    public AdminUserResponse revokeRole(Long userId, String roleCode, RoleChangeRequest request,
                                        ClientRequestInfo requestInfo) {
        Long actorId = requireAdministrator();
        requireNotSelf(actorId, userId);
        requireMutableUser(userId);
        RoleEntity role = roleRepository.findByRoleCodeAndActiveTrue(roleCode)
                .orElseThrow(() -> new ResourceNotFoundException("Active role was not found"));
        UserRoleEntity mapping = userRoleRepository.findById(new UserRoleId(userId, role.getId()))
                .filter(UserRoleEntity::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Active direct role assignment was not found"));
        if (userRoleRepository.countActiveByUserId(userId) <= 1) {
            throw new ConflictException("A user must retain at least one direct role");
        }
        mapping.revoke(actorId, clock.instant());
        tokenService.revokeAllRefreshTokens(userId);
        auditService.recordDomainChange(actorId, "ADMIN_USER_ROLE_REVOKED", "USER", userId,
                Map.of("role", roleCode), Map.of("reason", request.reason().trim()), requestInfo);
        return response(requireUser(userId));
    }

    private void requireStatusTransition(UserStatus oldStatus, UserStatus target) {
        boolean allowed = target == UserStatus.ACTIVE
                ? oldStatus == UserStatus.DEACTIVATED || oldStatus == UserStatus.SUSPENDED
                : (target == UserStatus.DEACTIVATED || target == UserStatus.SUSPENDED)
                && oldStatus == UserStatus.ACTIVE;
        if (!allowed) throw new ConflictException("Administrative user status transition is not allowed");
    }

    private AdminUserResponse response(UserEntity user) {
        return new AdminUserResponse(
                user.getId(), user.getFullName(), user.getDisplayName(), user.getEmail(), user.getPhone(),
                user.getStatus(), user.getEmailVerifiedAt(), user.getLastLoginAt(), user.getLockedUntil(),
                user.getDeactivatedAt(), user.getDeactivatedByUserId(), user.getVersion(),
                userRoleRepository.findActiveDirectRoleCodes(user.getId()),
                authorizationRepository.findEffectiveRoleCodes(user.getId()),
                RoleCode.labels()
        );
    }

    private UserEntity requireUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User was not found"));
    }

    private UserEntity requireMutableUser(Long id) {
        UserEntity user = requireUser(id);
        if (user.getStatus() == UserStatus.DELETED) {
            throw new ConflictException("Deleted users cannot receive role changes");
        }
        return user;
    }

    private Long requireAdministrator() {
        Long actorId = currentUserService.requireUserId();
        if (!authorizationRepository.findEffectiveRoleCodes(actorId).contains("ADMINISTRATOR")) {
            throw new AccessDeniedException("Administrator role is required");
        }
        return actorId;
    }

    private void requireNotSelf(Long actorId, Long targetId) {
        if (Objects.equals(actorId, targetId)) {
            throw new ConflictException("Administrators cannot change their own status or roles through this flow");
        }
    }
}
