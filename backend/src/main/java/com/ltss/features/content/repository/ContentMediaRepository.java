package com.ltss.features.content.repository;

import com.ltss.features.content.entity.ArticleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ContentMediaRepository extends JpaRepository<ArticleEntity, Long> {
    @Query(value = """
            SELECT mapping.article_id AS targetId, media.id AS mediaId, media.media_type AS mediaType,
                   media.media_url AS mediaUrl, media.thumbnail_url AS thumbnailUrl, media.mime_type AS mimeType,
                   mapping.usage_type AS usageType, mapping.display_order AS displayOrder, mapping.is_primary AS primaryMedia
            FROM article_media mapping JOIN media_assets media ON media.id = mapping.media_asset_id
            WHERE mapping.article_id IN (:ids) AND media.deleted_at IS NULL
            ORDER BY mapping.article_id, mapping.is_primary DESC, mapping.display_order, media.id
            """, nativeQuery = true)
    List<TargetMediaProjection> articleMedia(@Param("ids") Collection<Long> ids);

    @Query(value = """
            SELECT mapping.event_id AS targetId, media.id AS mediaId, media.media_type AS mediaType,
                   media.media_url AS mediaUrl, media.thumbnail_url AS thumbnailUrl, media.mime_type AS mimeType,
                   mapping.usage_type AS usageType, mapping.display_order AS displayOrder, mapping.is_primary AS primaryMedia
            FROM event_media mapping JOIN media_assets media ON media.id = mapping.media_asset_id
            WHERE mapping.event_id IN (:ids) AND media.deleted_at IS NULL
            ORDER BY mapping.event_id, mapping.is_primary DESC, mapping.display_order, media.id
            """, nativeQuery = true)
    List<TargetMediaProjection> eventMedia(@Param("ids") Collection<Long> ids);

    @Query(value = """
            SELECT mapping.business_post_id AS targetId, media.id AS mediaId, media.media_type AS mediaType,
                   media.media_url AS mediaUrl, media.thumbnail_url AS thumbnailUrl, media.mime_type AS mimeType,
                   mapping.usage_type AS usageType, mapping.display_order AS displayOrder, mapping.is_primary AS primaryMedia
            FROM business_post_media mapping JOIN media_assets media ON media.id = mapping.media_asset_id
            WHERE mapping.business_post_id IN (:ids) AND media.deleted_at IS NULL
            ORDER BY mapping.business_post_id, mapping.is_primary DESC, mapping.display_order, media.id
            """, nativeQuery = true)
    List<TargetMediaProjection> postMedia(@Param("ids") Collection<Long> ids);

    @Query(value = """
            SELECT mapping.promotion_id AS targetId, media.id AS mediaId, media.media_type AS mediaType,
                   media.media_url AS mediaUrl, media.thumbnail_url AS thumbnailUrl, media.mime_type AS mimeType,
                   mapping.usage_type AS usageType, mapping.display_order AS displayOrder, mapping.is_primary AS primaryMedia
            FROM promotion_media mapping JOIN media_assets media ON media.id = mapping.media_asset_id
            WHERE mapping.promotion_id IN (:ids) AND media.deleted_at IS NULL
            ORDER BY mapping.promotion_id, mapping.is_primary DESC, mapping.display_order, media.id
            """, nativeQuery = true)
    List<TargetMediaProjection> promotionMedia(@Param("ids") Collection<Long> ids);

    @Query(value = """
            SELECT mapping.business_post_id AS postId, tag.tag_name AS name, tag.slug AS slug
            FROM business_post_tags mapping JOIN tags tag ON tag.id = mapping.tag_id
            WHERE mapping.business_post_id IN (:ids)
            ORDER BY tag.tag_name
            """, nativeQuery = true)
    List<TagProjection> postTags(@Param("ids") Collection<Long> ids);
}
