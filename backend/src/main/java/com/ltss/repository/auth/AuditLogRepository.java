package com.ltss.repository.auth;

import com.ltss.entity.auth.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {
    @Query("""
            select log from AuditLogEntity log
            where log.actionCode not in :hiddenActions
              and (:actorId is null or log.actorUserId = :actorId)
              and (:action is null or lower(log.actionCode) like lower(concat('%', :action, '%')))
              and (:entityType is null or log.entityType = :entityType)
              and (:entityId is null or log.entityId = :entityId)
              and log.createdAt >= :fromTime and log.createdAt < :toTime
            order by log.createdAt desc, log.id desc
            """)
    Page<AuditLogEntity> search(
            @Param("hiddenActions") java.util.Collection<String> hiddenActions,
            @Param("actorId") Long actorId, @Param("action") String action,
            @Param("entityType") String entityType, @Param("entityId") Long entityId,
            @Param("fromTime") Instant from, @Param("toTime") Instant to, Pageable pageable);

    @Query("select min(log.createdAt) from AuditLogEntity log")
    Instant findOldestCreatedAt();
}
