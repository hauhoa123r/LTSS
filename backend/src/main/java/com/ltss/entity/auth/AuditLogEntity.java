package com.ltss.entity.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(name = "audit_logs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditLogEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "action_code", nullable = false, length = 100)
    private String actionCode;

    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "old_values", columnDefinition = "json")
    private Map<String, Object> oldValues;

    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "new_values", columnDefinition = "json")
    private Map<String, Object> newValues;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "request_id", length = 100)
    private String requestId;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public AuditLogEntity(
            Long actorUserId,
            String actionCode,
            Long entityId,
            String ipAddress,
            String requestId,
            Instant createdAt
    ) {
        this.actorUserId = actorUserId;
        this.actionCode = actionCode;
        this.entityType = "USER";
        this.entityId = entityId;
        this.ipAddress = ipAddress;
        this.requestId = requestId;
        this.createdAt = createdAt;
    }

    public AuditLogEntity(
            Long actorUserId,
            String actionCode,
            String entityType,
            Long entityId,
            String ipAddress,
            String requestId,
            Instant createdAt
    ) {
        this.actorUserId = actorUserId;
        this.actionCode = actionCode;
        this.entityType = entityType;
        this.entityId = entityId;
        this.ipAddress = ipAddress;
        this.requestId = requestId;
        this.createdAt = createdAt;
    }

    public AuditLogEntity(Long actorUserId, String actionCode, String entityType, Long entityId,
                          Map<String, Object> oldValues, Map<String, Object> newValues,
                          String ipAddress, String requestId, Instant createdAt) {
        this(actorUserId, actionCode, entityType, entityId, ipAddress, requestId, createdAt);
        this.oldValues = oldValues == null ? null : Map.copyOf(oldValues);
        this.newValues = newValues == null ? null : Map.copyOf(newValues);
    }
}
