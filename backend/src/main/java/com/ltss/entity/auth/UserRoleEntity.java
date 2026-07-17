package com.ltss.entity.auth;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;

@Getter
@Entity
@Table(name = "user_roles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserRoleEntity {
    @EmbeddedId
    private UserRoleId id;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "assigned_by_user_id")
    private Long assignedByUserId;

    @CreationTimestamp
    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    @Column(name = "revoked_by_user_id")
    private Long revokedByUserId;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    public UserRoleEntity(Long userId, Long roleId) {
        id = new UserRoleId(userId, roleId);
        active = true;
    }

    public UserRoleEntity(Long userId, Long roleId, Long assignedByUserId) {
        this(userId, roleId);
        this.assignedByUserId = assignedByUserId;
    }

    public void reactivate(Long actorId, Instant now) {
        active = true;
        assignedByUserId = actorId;
        assignedAt = now;
        revokedByUserId = null;
        revokedAt = null;
    }

    public void revoke(Long actorId, Instant now) {
        active = false;
        revokedByUserId = actorId;
        revokedAt = now;
    }
}
