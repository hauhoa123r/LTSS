package com.ltss.features.auth.service;

import com.ltss.features.auth.entity.AuditLogEntity;
import com.ltss.features.auth.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.Map;

@Service
public class AuditService {
    private final AuditLogRepository auditLogRepository;
    private final Clock clock;

    public AuditService(AuditLogRepository auditLogRepository, Clock clock) {
        this.auditLogRepository = auditLogRepository;
        this.clock = clock;
    }

    public void record(Long actorId, String actionCode, Long userId, ClientRequestInfo requestInfo) {
        auditLogRepository.save(new AuditLogEntity(
                actorId,
                actionCode,
                userId,
                requestInfo.ipAddress(),
                requestInfo.requestId(),
                clock.instant()
        ));
    }

    public void recordDomain(
            Long actorId,
            String actionCode,
            String entityType,
            Long entityId,
            ClientRequestInfo requestInfo
    ) {
        auditLogRepository.save(new AuditLogEntity(
                actorId, actionCode, entityType, entityId,
                requestInfo.ipAddress(), requestInfo.requestId(), clock.instant()
        ));
    }

    public void recordDomainChange(Long actorId, String actionCode, String entityType, Long entityId,
                                   Map<String, Object> oldValues, Map<String, Object> newValues,
                                   ClientRequestInfo requestInfo) {
        auditLogRepository.save(new AuditLogEntity(
                actorId, actionCode, entityType, entityId, oldValues, newValues,
                requestInfo.ipAddress(), requestInfo.requestId(), clock.instant()
        ));
    }
}
