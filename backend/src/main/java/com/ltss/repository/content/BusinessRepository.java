package com.ltss.repository.content;

import com.ltss.entity.content.BusinessEntity;
import com.ltss.entity.content.BusinessStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface BusinessRepository extends JpaRepository<BusinessEntity, Long> {
    @Query("""
            select business from BusinessEntity business
            join PlaceEntity place on place.id = business.placeId
            where business.status = com.ltss.entity.content.BusinessStatus.ACTIVE
              and place.status = com.ltss.entity.place.PlaceStatus.PUBLISHED
              and (:query is null or lower(place.name) like lower(concat('%', :query, '%')))
            order by place.name asc
            """)
    Page<BusinessEntity> searchActive(@Param("query") String query, Pageable pageable);

    Optional<BusinessEntity> findByIdAndStatus(Long id, BusinessStatus status);
    Optional<BusinessEntity> findByOwnerUserId(Long ownerUserId);
    long countByStatus(BusinessStatus status);

    @Query(value = """
            SELECT COUNT(*) FROM businesses b
            WHERE b.created_at >= :fromTime AND b.created_at < :toTime
            """, nativeQuery = true)
    long countCreatedInRange(@Param("fromTime") Instant from, @Param("toTime") Instant to);

    @Query(value = """
            SELECT b.status AS code, COUNT(*) AS value
            FROM businesses b
            GROUP BY b.status
            ORDER BY value DESC, code ASC
            """, nativeQuery = true)
    List<com.ltss.repository.analytics.MetricCountProjection> countByStatusCode();

    @Query(value = """
            SELECT
                pc.id AS categoryId,
                pc.category_name AS categoryName,
                pc.slug AS categorySlug,
                COUNT(b.id) AS totalBusinesses,
                COALESCE(SUM(CASE WHEN b.status = 'ACTIVE' THEN 1 ELSE 0 END), 0) AS activeBusinesses,
                COALESCE(SUM(CASE WHEN b.status = 'PENDING' THEN 1 ELSE 0 END), 0) AS pendingBusinesses,
                COALESCE(SUM(CASE WHEN b.status IN ('INACTIVE', 'SUSPENDED') THEN 1 ELSE 0 END), 0) AS inactiveOrSuspendedBusinesses
            FROM businesses b
            JOIN places p ON p.id = b.place_id
            JOIN place_categories pc ON pc.id = p.category_id
            GROUP BY pc.id, pc.category_name, pc.slug
            ORDER BY totalBusinesses DESC, activeBusinesses DESC, pc.category_name ASC
            """, nativeQuery = true)
    List<BusinessCategoryStatisticsProjection> countByCategory();

    @Query(value = """
            SELECT
                b.id AS businessId,
                p.name AS businessName,
                p.slug AS placeSlug,
                pc.category_name AS categoryName,
                pc.slug AS categorySlug,
                b.status AS status,
                b.created_at AS createdAt,
                b.approved_at AS approvedAt
            FROM businesses b
            JOIN places p ON p.id = b.place_id
            JOIN place_categories pc ON pc.id = p.category_id
            ORDER BY b.created_at DESC, b.id DESC
            """, nativeQuery = true)
    List<BusinessAccountStatisticsProjection> businessAccountsForStatistics();
}
