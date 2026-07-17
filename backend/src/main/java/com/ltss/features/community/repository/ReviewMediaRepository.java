package com.ltss.features.community.repository;

import com.ltss.features.community.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface ReviewMediaRepository extends JpaRepository<ReviewMediaEntity, ReviewMediaId> {
    @Query(value = """
            SELECT rm.review_id AS reviewId, ma.id AS mediaId, ma.media_url AS mediaUrl,
                   ma.thumbnail_url AS thumbnailUrl, rm.display_order AS displayOrder
            FROM review_media rm JOIN media_assets ma ON ma.id = rm.media_asset_id
            WHERE rm.review_id IN (:reviewIds) AND ma.deleted_at IS NULL
            ORDER BY rm.review_id, rm.display_order
            """, nativeQuery = true)
    List<ReviewMediaProjection> findMedia(@Param("reviewIds") Collection<Long> reviewIds);

    @Query(value = """
            SELECT id FROM media_assets
            WHERE id IN (:ids) AND media_type = 'IMAGE' AND deleted_at IS NULL
            """, nativeQuery = true)
    List<Long> findValidImageIds(@Param("ids") Collection<Long> ids);
}
