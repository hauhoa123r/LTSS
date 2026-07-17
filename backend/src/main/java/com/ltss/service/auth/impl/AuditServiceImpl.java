package com.ltss.service.auth.impl;

import com.ltss.service.auth.AuditService;
import com.ltss.service.auth.ClientRequestInfo;

import com.ltss.entity.auth.AuditLogEntity;
import com.ltss.repository.auth.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.Map;

@Service
public class AuditServiceImpl implements AuditService {
    private final AuditLogRepository auditLogRepository;
    private final Clock clock;

    public AuditServiceImpl(AuditLogRepository auditLogRepository, Clock clock) {
        this.auditLogRepository = auditLogRepository;
        this.clock = clock;
    }

    @Override
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

    @Override
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

    @Override
    public void recordDomainChange(Long actorId, String actionCode, String entityType, Long entityId,
                                   Map<String, Object> oldValues, Map<String, Object> newValues,
                                   ClientRequestInfo requestInfo) {
        auditLogRepository.save(new AuditLogEntity(
                actorId, actionCode, entityType, entityId, oldValues, newValues,
                requestInfo.ipAddress(), requestInfo.requestId(), clock.instant()
        ));
    }
}
