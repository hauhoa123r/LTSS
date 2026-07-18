package com.ltss.repository.analytics;

import com.ltss.entity.analytics.EngagementEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface EngagementEventRepository extends JpaRepository<EngagementEventEntity, Long> {
    long countBySessionKeyAndOccurredAtGreaterThanEqual(String sessionKey, Instant from);

    @Query("""
            select count(event) from EngagementEventEntity event
            where event.eventTypeCode = :eventType
              and event.sessionKey = :sessionKey
              and event.occurredAt >= :from
              and ((:targetType = 'PLACE' and event.placeId = :targetId)
                or (:targetType = 'BUSINESS' and event.businessId = :targetId)
                or (:targetType = 'EVENT' and event.eventId = :targetId)
                or (:targetType = 'ARTICLE' and event.articleId = :targetId)
                or (:targetType = 'BUSINESS_POST' and event.businessPostId = :targetId)
                or (:targetType = 'PROMOTION' and event.promotionId = :targetId)
                or (:targetType = 'TOUR' and event.tourId = :targetId))
            """)
    long countDuplicate(
            @Param("eventType") String eventType,
            @Param("sessionKey") String sessionKey,
            @Param("targetType") String targetType,
            @Param("targetId") Long targetId,
            @Param("from") Instant from
    );

    @Query("select min(event.occurredAt) from EngagementEventEntity event")
    Instant findOldestOccurredAt();

    @Query(value = """
            SELECT COUNT(*) FROM engagement_events e
            WHERE e.occurred_at >= :fromTime AND e.occurred_at < :toTime
            """, nativeQuery = true)
    long countInRange(@Param("fromTime") Instant from, @Param("toTime") Instant to);

    @Query(value = """
            SELECT COUNT(DISTINCT e.session_key) FROM engagement_events e
            WHERE e.occurred_at >= :fromTime AND e.occurred_at < :toTime
            """, nativeQuery = true)
    long countUniqueSessions(@Param("fromTime") Instant from, @Param("toTime") Instant to);

    @Query(value = """
            SELECT COUNT(DISTINCT e.user_id) FROM engagement_events e
            WHERE e.user_id IS NOT NULL
              AND e.occurred_at >= :fromTime AND e.occurred_at < :toTime
            """, nativeQuery = true)
    long countAuthenticatedUsers(@Param("fromTime") Instant from, @Param("toTime") Instant to);

    @Query(value = """
            SELECT e.event_type_code AS code, COUNT(*) AS value
            FROM engagement_events e
            WHERE e.occurred_at >= :fromTime AND e.occurred_at < :toTime
            GROUP BY e.event_type_code ORDER BY value DESC, code ASC
            """, nativeQuery = true)
    List<MetricCountProjection> countByType(@Param("fromTime") Instant from, @Param("toTime") Instant to);

    @Query(value = """
            SELECT DATE(e.occurred_at) AS day, COUNT(*) AS value
            FROM engagement_events e
            WHERE e.occurred_at >= :fromTime AND e.occurred_at < :toTime
            GROUP BY DATE(e.occurred_at) ORDER BY day ASC
            """, nativeQuery = true)
    List<DailyCountProjection> countDaily(@Param("fromTime") Instant from, @Param("toTime") Instant to);

    @Query(value = """
            SELECT DATE(e.occurred_at) AS day, COUNT(DISTINCT e.session_key) AS value
            FROM engagement_events e
            JOIN events event ON event.id = e.event_id
            WHERE event.status = 'PUBLISHED'
              AND event.start_at < :toTime AND event.end_at >= :fromTime
              AND e.occurred_at >= :fromTime AND e.occurred_at < :toTime
            GROUP BY DATE(e.occurred_at) ORDER BY day ASC
            """, nativeQuery = true)
    List<DailyCountProjection> countEventRegistrationsDaily(@Param("fromTime") Instant from, @Param("toTime") Instant to);

    @Query(value = """
            SELECT COUNT(*) FROM engagement_events e
            WHERE e.event_type_code = 'PLACE_VIEW'
              AND e.place_id IS NOT NULL
              AND e.occurred_at >= :fromTime AND e.occurred_at < :toTime
            """, nativeQuery = true)
    long countPlaceViews(@Param("fromTime") Instant from, @Param("toTime") Instant to);

    @Query(value = """
            SELECT COUNT(DISTINCT e.session_key) FROM engagement_events e
            WHERE e.event_type_code = 'PLACE_VIEW'
              AND e.place_id IS NOT NULL
              AND e.occurred_at >= :fromTime AND e.occurred_at < :toTime
            """, nativeQuery = true)
    long countPlaceViewSessions(@Param("fromTime") Instant from, @Param("toTime") Instant to);

    @Query(value = """
            SELECT COUNT(DISTINCT e.user_id) FROM engagement_events e
            WHERE e.event_type_code = 'PLACE_VIEW'
              AND e.user_id IS NOT NULL
              AND e.place_id IS NOT NULL
              AND e.occurred_at >= :fromTime AND e.occurred_at < :toTime
            """, nativeQuery = true)
    long countPlaceViewUsers(@Param("fromTime") Instant from, @Param("toTime") Instant to);

    @Query(value = """
            SELECT DATE(e.occurred_at) AS day, COUNT(*) AS value
            FROM engagement_events e
            WHERE e.event_type_code = 'PLACE_VIEW'
              AND e.place_id IS NOT NULL
              AND e.occurred_at >= :fromTime AND e.occurred_at < :toTime
            GROUP BY DATE(e.occurred_at) ORDER BY day ASC
            """, nativeQuery = true)
    List<DailyCountProjection> countPlaceViewsDaily(@Param("fromTime") Instant from, @Param("toTime") Instant to);

    @Query(value = """
            SELECT DATE(DATE_SUB(e.occurred_at, INTERVAL WEEKDAY(e.occurred_at) DAY)) AS day, COUNT(*) AS value
            FROM engagement_events e
            WHERE e.event_type_code = 'PLACE_VIEW'
              AND e.place_id IS NOT NULL
              AND e.occurred_at >= :fromTime AND e.occurred_at < :toTime
            GROUP BY DATE(DATE_SUB(e.occurred_at, INTERVAL WEEKDAY(e.occurred_at) DAY)) ORDER BY day ASC
            """, nativeQuery = true)
    List<DailyCountProjection> countPlaceViewsWeekly(@Param("fromTime") Instant from, @Param("toTime") Instant to);

    @Query(value = """
            SELECT CAST(DATE_FORMAT(e.occurred_at, '%Y-%m-01') AS DATE) AS day, COUNT(*) AS value
            FROM engagement_events e
            WHERE e.event_type_code = 'PLACE_VIEW'
              AND e.place_id IS NOT NULL
              AND e.occurred_at >= :fromTime AND e.occurred_at < :toTime
            GROUP BY CAST(DATE_FORMAT(e.occurred_at, '%Y-%m-01') AS DATE) ORDER BY day ASC
            """, nativeQuery = true)
    List<DailyCountProjection> countPlaceViewsMonthly(@Param("fromTime") Instant from, @Param("toTime") Instant to);

    @Query(value = """
            SELECT
                p.id AS placeId,
                p.name AS name,
                p.slug AS slug,
                p.address AS address,
                COUNT(e.id) AS visits,
                COUNT(DISTINCT e.session_key) AS uniqueSessions,
                COUNT(DISTINCT e.user_id) AS authenticatedVisitors,
                MAX(e.occurred_at) AS lastVisitAt
            FROM places p
            LEFT JOIN engagement_events e
              ON e.place_id = p.id
             AND e.event_type_code = 'PLACE_VIEW'
             AND e.occurred_at >= :fromTime AND e.occurred_at < :toTime
            WHERE p.status = 'PUBLISHED'
            GROUP BY p.id, p.name, p.slug, p.address
            ORDER BY visits DESC, uniqueSessions DESC, p.name ASC
            """, nativeQuery = true)
    List<MonumentVisitProjection> countVisitsByMonument(@Param("fromTime") Instant from, @Param("toTime") Instant to);

    @Query(value = """
            SELECT e.place_id AS placeId, COUNT(*) AS visits
            FROM engagement_events e
            WHERE e.event_type_code = 'PLACE_VIEW'
              AND e.place_id IS NOT NULL
              AND e.occurred_at >= :fromTime AND e.occurred_at < :toTime
            GROUP BY e.place_id
            """, nativeQuery = true)
    List<MonumentCountProjection> countPlaceViewsByMonument(@Param("fromTime") Instant from, @Param("toTime") Instant to);

    @Query(value = """
            SELECT e.place_id AS placeId, DATE(e.occurred_at) AS day, COUNT(*) AS value
            FROM engagement_events e
            WHERE e.event_type_code = 'PLACE_VIEW'
              AND e.place_id IS NOT NULL
              AND e.occurred_at >= :fromTime AND e.occurred_at < :toTime
            GROUP BY e.place_id, DATE(e.occurred_at)
            ORDER BY e.place_id ASC, day ASC
            """, nativeQuery = true)
    List<MonumentTrendProjection> countPlaceViewsDailyByMonument(@Param("fromTime") Instant from, @Param("toTime") Instant to);

    @Query(value = """
            SELECT COUNT(*) FROM engagement_events e
            WHERE e.occurred_at >= :fromTime AND e.occurred_at < :toTime
              AND (e.business_id = :businessId OR e.place_id = :placeId
                OR e.business_post_id IN (SELECT p.id FROM business_posts p WHERE p.business_id = :businessId)
                OR e.promotion_id IN (SELECT p.id FROM promotions p WHERE p.business_id = :businessId))
            """, nativeQuery = true)
    long countBusinessEvents(@Param("businessId") Long businessId, @Param("placeId") Long placeId,
                             @Param("fromTime") Instant from, @Param("toTime") Instant to);

    @Query(value = """
            SELECT COUNT(DISTINCT e.session_key) FROM engagement_events e
            WHERE e.occurred_at >= :fromTime AND e.occurred_at < :toTime
              AND (e.business_id = :businessId OR e.place_id = :placeId
                OR e.business_post_id IN (SELECT p.id FROM business_posts p WHERE p.business_id = :businessId)
                OR e.promotion_id IN (SELECT p.id FROM promotions p WHERE p.business_id = :businessId))
            """, nativeQuery = true)
    long countBusinessSessions(@Param("businessId") Long businessId, @Param("placeId") Long placeId,
                               @Param("fromTime") Instant from, @Param("toTime") Instant to);

    @Query(value = """
            SELECT COUNT(DISTINCT e.user_id) FROM engagement_events e
            WHERE e.user_id IS NOT NULL AND e.occurred_at >= :fromTime AND e.occurred_at < :toTime
              AND (e.business_id = :businessId OR e.place_id = :placeId
                OR e.business_post_id IN (SELECT p.id FROM business_posts p WHERE p.business_id = :businessId)
                OR e.promotion_id IN (SELECT p.id FROM promotions p WHERE p.business_id = :businessId))
            """, nativeQuery = true)
    long countBusinessUsers(@Param("businessId") Long businessId, @Param("placeId") Long placeId,
                            @Param("fromTime") Instant from, @Param("toTime") Instant to);

    @Query(value = """
            SELECT e.event_type_code AS code, COUNT(*) AS value FROM engagement_events e
            WHERE e.occurred_at >= :fromTime AND e.occurred_at < :toTime
              AND (e.business_id = :businessId OR e.place_id = :placeId
                OR e.business_post_id IN (SELECT p.id FROM business_posts p WHERE p.business_id = :businessId)
                OR e.promotion_id IN (SELECT p.id FROM promotions p WHERE p.business_id = :businessId))
            GROUP BY e.event_type_code ORDER BY value DESC, code ASC
            """, nativeQuery = true)
    List<MetricCountProjection> countBusinessByType(
            @Param("businessId") Long businessId, @Param("placeId") Long placeId,
            @Param("fromTime") Instant from, @Param("toTime") Instant to);

    @Query(value = """
            SELECT DATE(e.occurred_at) AS day, COUNT(*) AS value FROM engagement_events e
            WHERE e.occurred_at >= :fromTime AND e.occurred_at < :toTime
              AND (e.business_id = :businessId OR e.place_id = :placeId
                OR e.business_post_id IN (SELECT p.id FROM business_posts p WHERE p.business_id = :businessId)
                OR e.promotion_id IN (SELECT p.id FROM promotions p WHERE p.business_id = :businessId))
            GROUP BY DATE(e.occurred_at) ORDER BY day ASC
            """, nativeQuery = true)
    List<DailyCountProjection> countBusinessDaily(
            @Param("businessId") Long businessId, @Param("placeId") Long placeId,
            @Param("fromTime") Instant from, @Param("toTime") Instant to);
}
