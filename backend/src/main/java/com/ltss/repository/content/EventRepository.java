package com.ltss.repository.content;

import com.ltss.entity.content.EventEntity;
import com.ltss.entity.content.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<EventEntity, Long> {
    @Query("""
            select event from EventEntity event
            where event.status = com.ltss.entity.content.EventStatus.PUBLISHED
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

    @Query(value = """
            SELECT
                event.id AS eventId,
                event.title AS title,
                event.slug AS slug,
                place.name AS placeName,
                event.location_note AS locationNote,
                event.start_at AS startAt,
                event.end_at AS endAt,
                CASE
                    WHEN event.end_at < :nowTime THEN 'HISTORICAL'
                    WHEN event.start_at <= :nowTime AND event.end_at >= :nowTime THEN 'ACTIVE'
                    ELSE 'UPCOMING'
                END AS periodStatus,
                COUNT(DISTINCT engagement.session_key) AS participantRegistrations,
                COUNT(DISTINCT engagement.user_id) AS authenticatedParticipants,
                COUNT(engagement.id) AS engagementCount
            FROM events event
            LEFT JOIN places place ON place.id = event.place_id
            LEFT JOIN engagement_events engagement
              ON engagement.event_id = event.id
             AND engagement.occurred_at >= :fromTime
             AND engagement.occurred_at < :toTime
            WHERE event.status = 'PUBLISHED'
              AND event.start_at < :toTime
              AND event.end_at >= :fromTime
            GROUP BY event.id, event.title, event.slug, place.name, event.location_note,
                     event.start_at, event.end_at
            ORDER BY event.start_at ASC, event.title ASC
            """, nativeQuery = true)
    List<MonthlyEventStatisticsProjection> monthlyEventStatistics(
            @Param("fromTime") Instant from,
            @Param("toTime") Instant to,
            @Param("nowTime") Instant now
    );
}
