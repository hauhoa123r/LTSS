package com.ltss.features.content.dto;

import com.ltss.features.content.entity.ArticleCategoryEntity;

public record ArticleCategoryResponse(Long id, String name, String slug, String description) {
    public static ArticleCategoryResponse from(ArticleCategoryEntity category) {
        return new ArticleCategoryResponse(
                category.getId(), category.getCategoryName(), category.getSlug(), category.getDescription()
        );
    }
}
