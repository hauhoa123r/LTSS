package com.ltss.entity.content;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Entity
@Table(name = "article_categories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArticleCategoryEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "category_name", nullable = false, length = 100)
    private String categoryName;
    @Column(nullable = false, length = 120)
    private String slug;
    @Column(length = 500)
    private String description;
    @Column(name = "is_active", nullable = false)
    private boolean active;
    @Column(name = "created_by_user_id")
    private Long createdByUserId;
    @Column(name = "updated_by_user_id")
    private Long updatedByUserId;
    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    public ArticleCategoryEntity(
            String categoryName, String slug, String description, boolean active, Long actorId
    ) {
        this.categoryName = categoryName;
        this.slug = slug;
        this.description = description;
        this.active = active;
        this.createdByUserId = actorId;
        this.updatedByUserId = actorId;
    }

    public void update(
            String categoryName, String slug, String description, boolean active, Long actorId
    ) {
        this.categoryName = categoryName;
        this.slug = slug;
        this.description = description;
        this.active = active;
        this.updatedByUserId = actorId;
    }

    public void deactivate(Long actorId) {
        this.active = false;
        this.updatedByUserId = actorId;
    }
}
