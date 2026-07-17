package com.ltss.features.place.repository;

import com.ltss.features.place.entity.FavoriteEntity;
import com.ltss.features.place.entity.FavoriteId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface FavoriteRepository extends JpaRepository<FavoriteEntity, FavoriteId> {

    @org.springframework.data.jpa.repository.Modifying
    @Query(value = """
            INSERT IGNORE INTO favorites (user_id, place_id)
            VALUES (:userId, :placeId)
            """, nativeQuery = true)
    int addIfAbsent(@Param("userId") Long userId, @Param("placeId") Long placeId);

    @Query("""
            select favorite.id.placeId
            from FavoriteEntity favorite
            where favorite.id.userId = :userId
              and favorite.id.placeId in :placeIds
            """)
    List<Long> findFavoritePlaceIds(
            @Param("userId") Long userId,
            @Param("placeIds") Collection<Long> placeIds
    );

    @Query(value = """
            SELECT favorite.place_id
            FROM favorites favorite
            JOIN places place ON place.id = favorite.place_id
            JOIN place_categories category ON category.id = place.category_id
            WHERE favorite.user_id = :userId
              AND place.status = 'PUBLISHED'
              AND category.is_active = TRUE
            ORDER BY favorite.created_at DESC
            """, countQuery = """
            SELECT COUNT(*)
            FROM favorites favorite
            JOIN places place ON place.id = favorite.place_id
            JOIN place_categories category ON category.id = place.category_id
            WHERE favorite.user_id = :userId
              AND place.status = 'PUBLISHED'
              AND category.is_active = TRUE
            """, nativeQuery = true)
    Page<Long> findPublishedPlaceIds(@Param("userId") Long userId, Pageable pageable);

    @org.springframework.data.jpa.repository.Modifying
    @Query("delete from FavoriteEntity favorite where favorite.id.userId = :userId and favorite.id.placeId = :placeId")
    int deleteOwned(@Param("userId") Long userId, @Param("placeId") Long placeId);
}
