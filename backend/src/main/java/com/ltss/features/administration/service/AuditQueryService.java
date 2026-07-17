package com.ltss.features.administration.service;

import com.ltss.common.exception.BusinessRuleViolationException;
import com.ltss.common.response.PageResponse;
import com.ltss.features.administration.dto.AuditLogResponse;
import com.ltss.features.auth.entity.AuditLogEntity;
import com.ltss.features.auth.repository.*;
import com.ltss.features.auth.security.CurrentUserService;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
public class AuditQueryService {
    private static final Set<String> SENSITIVE = Set.of(
            "password", "token", "secret", "authorization", "cookie", "otp", "hash"
    );
    private final AuditLogRepository repository;
    private final AuthorizationRepository authorizationRepository;
    private final CurrentUserService currentUserService;

    public AuditQueryService(AuditLogRepository repository, AuthorizationRepository authorizationRepository,
                             CurrentUserService currentUserService) {
        this.repository = repository;
        this.authorizationRepository = authorizationRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
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
        return PageResponse.from(repository.search(
                actorId, normalizedAction, normalizedEntity, entityId, fromTime, toTime, PageRequest.of(page, size)
        ).map(this::response));
    }

    private AuditLogResponse response(AuditLogEntity log) {
        return new AuditLogResponse(
                log.getId(), log.getActorUserId(), log.getActionCode(), log.getEntityType(),
                log.getEntityId(), safeValues(log.getOldValues()), safeValues(log.getNewValues()),
                log.getIpAddress(), log.getRequestId(), log.getCreatedAt()
        );
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
