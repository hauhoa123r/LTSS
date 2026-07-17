package com.ltss.service.auth;

import java.util.Map;

public interface AuditService {

    void record(Long actorId, String actionCode, Long userId, ClientRequestInfo requestInfo);

    void recordDomain(
            Long actorId,
            String actionCode,
            String entityType,
            Long entityId,
            ClientRequestInfo requestInfo
    );

    void recordDomainChange(Long actorId, String actionCode, String entityType, Long entityId,
                                   Map<String, Object> oldValues, Map<String, Object> newValues,
                                   ClientRequestInfo requestInfo);
}
