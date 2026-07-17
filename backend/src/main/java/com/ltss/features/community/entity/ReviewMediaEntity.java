package com.ltss.features.community.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "review_media")
@IdClass(ReviewMediaId.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewMediaEntity {
    @Id @Column(name = "review_id") private Long reviewId;
    @Id @Column(name = "media_asset_id") private Long mediaAssetId;
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "display_order", nullable = false) private Short displayOrder;

    public ReviewMediaEntity(Long reviewId, Long mediaAssetId, int order) {
        this.reviewId = reviewId;
        this.mediaAssetId = mediaAssetId;
        this.displayOrder = (short) order;
    }
}
