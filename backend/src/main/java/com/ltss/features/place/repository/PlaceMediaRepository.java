package com.ltss.features.place.repository;

import com.ltss.features.place.entity.PlaceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PlaceMediaRepository extends JpaRepository<PlaceEntity, Long> {
    @Query(value = """
            SELECT
                pm.place_id AS placeId,
                ma.id AS mediaId,
                ma.media_type AS mediaType,
                ma.media_url AS mediaUrl,
                ma.thumbnail_url AS thumbnailUrl,
                ma.mime_type AS mimeType,
                ma.file_size_bytes AS fileSizeBytes,
                ma.width_px AS widthPx,
                ma.height_px AS heightPx,
                ma.duration_seconds AS durationSeconds,
                pm.usage_type AS usageType,
                pm.display_order AS displayOrder,
                pm.is_primary AS primaryMedia
            FROM place_media pm
            JOIN media_assets ma ON ma.id = pm.media_asset_id
            WHERE pm.place_id IN (:placeIds)
              AND ma.deleted_at IS NULL
            ORDER BY pm.place_id, pm.is_primary DESC, pm.display_order ASC, ma.id ASC
            """, nativeQuery = true)
    List<PlaceMediaProjection> findMediaForPlaces(@Param("placeIds") Collection<Long> placeIds);

    @Query(value = """
            SELECT
                hotspot.id AS id,
                hotspot.source_media_asset_id AS sourceMediaAssetId,
                hotspot.target_media_asset_id AS targetMediaAssetId,
                hotspot.hotspot_type AS hotspotType,
                hotspot.yaw_degrees AS yawDegrees,
                hotspot.pitch_degrees AS pitchDegrees,
                hotspot.label AS label,
                hotspot.description AS description,
                hotspot.display_order AS displayOrder
            FROM panorama_hotspots hotspot
            JOIN place_media source_mapping
              ON source_mapping.media_asset_id = hotspot.source_media_asset_id
            LEFT JOIN place_media target_mapping
              ON target_mapping.media_asset_id = hotspot.target_media_asset_id
             AND target_mapping.place_id = source_mapping.place_id
            WHERE source_mapping.place_id = :placeId
              AND hotspot.is_active = TRUE
              AND (hotspot.hotspot_type = 'INFO' OR target_mapping.place_id IS NOT NULL)
            ORDER BY hotspot.source_media_asset_id, hotspot.display_order
            """, nativeQuery = true)
    List<HotspotProjection> findActiveHotspots(@Param("placeId") Long placeId);
}
