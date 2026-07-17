package com.ltss.features.content.repository;

import com.ltss.features.content.entity.EventEntity;
import com.ltss.features.content.entity.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.time.Instant;
import java.util.Optional;

public interface EventRepository extends JpaRepository<EventEntity, Long> {
    @Query("""
            select event from EventEntity event
            where event.status = com.ltss.features.content.entity.EventStatus.PUBLISHED
              and event.endAt >= :now
              and (:query is null or lower(event.title) like lower(concat('%', :query, '%')))
            order by event.startAt asc
            """)
    Page<EventEntity> findUpcoming(
            @Param("query") String query,
            @Param("now") Instant now,
            Pageable pageable
    );

    Optional<EventEntity> findBySlugAndStatus(String slug, EventStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select event from EventEntity event where event.id = :id")
    Optional<EventEntity> findLockedById(@Param("id") Long id);
}
