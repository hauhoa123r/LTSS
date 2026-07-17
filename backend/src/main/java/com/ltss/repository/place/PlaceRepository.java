package com.ltss.repository.place;

import com.ltss.entity.place.PlaceEntity;
import com.ltss.entity.place.PlaceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PlaceRepository extends JpaRepository<PlaceEntity, Long> {

    long countByStatus(PlaceStatus status);

    @Query("""
            select place
            from PlaceEntity place
            join PlaceCategoryEntity category on category.id = place.categoryId
            where place.status = com.ltss.entity.place.PlaceStatus.PUBLISHED
              and category.active = true
              and (:categorySlug is null or category.slug = :categorySlug)
              and (:query is null
                   or lower(place.name) like lower(concat('%', :query, '%'))
                   or lower(coalesce(place.summary, '')) like lower(concat('%', :query, '%'))
                   or lower(coalesce(place.description, '')) like lower(concat('%', :query, '%')))
            order by
              case when :query is not null and lower(place.name) = lower(:query) then 0 else 1 end,
              place.name asc
            """)
    Page<PlaceEntity> searchPublished(
            @Param("query") String query,
            @Param("categorySlug") String categorySlug,
            Pageable pageable
    );

    Optional<PlaceEntity> findBySlugAndStatus(String slug, PlaceStatus status);

    List<PlaceEntity> findAllByIdInAndStatus(Collection<Long> ids, PlaceStatus status);

    @Query(value = """
            SELECT
                p.id AS placeId,
                p.category_id AS categoryId,
                p.name AS name,
                p.slug AS slug,
                p.summary AS summary,
                p.address AS address,
                p.latitude AS latitude,
                p.longitude AS longitude,
                p.entrance_fee AS entranceFee,
                6371.0 * 2 * ASIN(SQRT(
                    POWER(SIN(RADIANS(p.latitude - :latitude) / 2), 2) +
                    COS(RADIANS(:latitude)) * COS(RADIANS(p.latitude)) *
                    POWER(SIN(RADIANS(p.longitude - :longitude) / 2), 2)
                )) AS distanceKm
            FROM places p
            JOIN place_categories c ON c.id = p.category_id AND c.is_active = TRUE
            WHERE p.status = 'PUBLISHED'
              AND p.latitude IS NOT NULL
              AND p.longitude IS NOT NULL
              AND (:categorySlug IS NULL OR c.slug = :categorySlug)
            HAVING distanceKm <= :radiusKm
            ORDER BY distanceKm ASC, p.name ASC
            """, countQuery = """
            SELECT COUNT(*)
            FROM places p
            JOIN place_categories c ON c.id = p.category_id AND c.is_active = TRUE
            WHERE p.status = 'PUBLISHED'
              AND p.latitude IS NOT NULL
              AND p.longitude IS NOT NULL
              AND (:categorySlug IS NULL OR c.slug = :categorySlug)
              AND 6371.0 * 2 * ASIN(SQRT(
                    POWER(SIN(RADIANS(p.latitude - :latitude) / 2), 2) +
                    COS(RADIANS(:latitude)) * COS(RADIANS(p.latitude)) *
                    POWER(SIN(RADIANS(p.longitude - :longitude) / 2), 2)
                  )) <= :radiusKm
            """, nativeQuery = true)
    Page<NearbyPlaceProjection> findNearby(
            @Param("latitude") double latitude,
            @Param("longitude") double longitude,
            @Param("radiusKm") double radiusKm,
            @Param("categorySlug") String categorySlug,
            Pageable pageable
    );
}
