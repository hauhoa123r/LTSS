package com.ltss.repository.analytics;

import com.ltss.entity.analytics.EngagementEventTypeEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EngagementEventTypeRepository extends JpaRepository<EngagementEventTypeEntity, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select type from EngagementEventTypeEntity type where type.eventTypeCode = :code")
    Optional<EngagementEventTypeEntity> findLockedByCode(@Param("code") String code);
}
