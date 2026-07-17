package com.ltss.service.administration.impl;

import com.ltss.service.administration.AuditEntityDisplayNameResolver;
import com.ltss.service.administration.AuditQueryService;

import com.ltss.common.exception.BusinessRuleViolationException;
import com.ltss.common.exception.ResourceNotFoundException;
import com.ltss.common.response.PageResponse;
import com.ltss.dto.administration.AuditLogResponse;
import com.ltss.dto.administration.AuditMetadataResponse;
import com.ltss.dto.administration.model.*;
import com.ltss.entity.auth.AuditLogEntity;
import com.ltss.repository.auth.*;
import com.ltss.security.auth.CurrentUserService;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
public class AuditQueryServiceImpl implements AuditQueryService {
    private static final Set<String> SENSITIVE = Set.of(
            "password", "token", "secret", "authorization", "cookie", "otp", "hash"
    );
    private final AuditLogRepository repository;
    private final UserRepository userRepository;
    private final AuthorizationRepository authorizationRepository;
    private final CurrentUserService currentUserService;
    private final AuditEntityDisplayNameResolver entityDisplayNameResolver;

    public AuditQueryServiceImpl(AuditLogRepository repository, UserRepository userRepository,
                             AuthorizationRepository authorizationRepository, CurrentUserService currentUserService,
                             AuditEntityDisplayNameResolver entityDisplayNameResolver) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.authorizationRepository = authorizationRepository;
        this.currentUserService = currentUserService;
        this.entityDisplayNameResolver = entityDisplayNameResolver;
    }

    @Transactional(readOnly = true)
    @Override
    public PageResponse<AuditLogResponse> search(Long actorId, String action, String entityType, Long entityId,
                                                 LocalDate from, LocalDate to, int page, int size) {
        requireAdministrator();
        if (from == null || to == null || from.isAfter(to)) {
            throw new BusinessRuleViolationException("Audit date range is invalid");
        }
        Instant fromTime = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toTime = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        String normalizedAction = normalize(action);
        String normalizedEntity = normalize(entityType);
        if (normalizedEntity != null) normalizedEntity = normalizedEntity.toUpperCase(Locale.ROOT);
        var logs = repository.search(
                AuditActionCode.hiddenCodes(), actorId, normalizedAction, normalizedEntity, entityId,
                fromTime, toTime, PageRequest.of(page, size));
        Set<Long> actorIds = new LinkedHashSet<>();
        logs.forEach(log -> {
            if (log.getActorUserId() != null) actorIds.add(log.getActorUserId());
        });
        Map<Long, String> actorNames = new HashMap<>();
        userRepository.findAllById(actorIds).forEach(user -> actorNames.put(user.getId(), user.getDisplayName()));
        Map<Long, String> entityNames = entityDisplayNameResolver.resolve(logs.getContent());
        return PageResponse.from(logs.map(log -> response(
                log, actorNames.get(log.getActorUserId()),
                entityNames.get(log.getId()))));
    }

    @Transactional(readOnly = true)
    @Override
    public AuditMetadataResponse metadata() {
        requireAdministrator();
        return new AuditMetadataResponse(AuditActionCode.visibleLabels(), AuditEntityType.labels());
    }

    @Transactional(readOnly = true)
    @Override
    public AuditLogResponse detail(Long auditLogId) {
        requireAdministrator();
        AuditLogEntity log = repository.findById(auditLogId)
                .orElseThrow(() -> new ResourceNotFoundException("Audit log was not found"));
        if (AuditActionCode.hiddenCodes().contains(log.getActionCode())) {
            throw new ResourceNotFoundException("Audit log was not found");
        }
        String actorDisplayName = log.getActorUserId() == null ? null : userRepository.findById(log.getActorUserId())
                .map(user -> user.getDisplayName()).orElse(null);
        String entityDisplayName = entityDisplayNameResolver.resolve(List.of(log)).get(log.getId());
        return response(log, actorDisplayName, entityDisplayName);
    }

    private AuditLogResponse response(AuditLogEntity log, String actorDisplayName, String entityDisplayName) {
        Map<String, Object> oldValues = safeValues(log.getOldValues());
        Map<String, Object> newValues = safeValues(log.getNewValues());
        return new AuditLogResponse(
                log.getId(), log.getActorUserId(), actorDisplayName,
                log.getActionCode(), AuditActionCode.labelOf(log.getActionCode()),
                log.getEntityType(), AuditEntityType.labelOf(log.getEntityType()), log.getEntityId(), entityDisplayName,
                oldValues, newValues, displayValues(oldValues), displayValues(newValues),
                log.getIpAddress(), log.getRequestId(), log.getCreatedAt()
        );
    }

    private Map<String, Object> displayValues(Map<String, Object> values) {
        if (values.isEmpty()) return Map.of();
        Map<String, Object> display = new LinkedHashMap<>();
        values.forEach((key, value) -> display.put(AuditFieldCode.labelOf(key), AuditValueCode.labelOf(value)));
        return display;
    }

    private Map<String, Object> safeValues(Map<String, Object> values) {
        if (values == null || values.isEmpty()) return Map.of();
        Map<String, Object> safe = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            String lower = key == null ? "" : key.toLowerCase(Locale.ROOT);
            if (SENSITIVE.stream().anyMatch(lower::contains)) return;
            if (value instanceof String text) safe.put(key, text.length() > 500 ? text.substring(0, 500) : text);
            else if (value instanceof Number || value instanceof Boolean) safe.put(key, value);
        });
        return safe;
    }

    private void requireAdministrator() {
        Long actorId = currentUserService.requireUserId();
        if (!authorizationRepository.findEffectiveRoleCodes(actorId).contains("ADMINISTRATOR")) {
            throw new AccessDeniedException("Administrator role is required");
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
