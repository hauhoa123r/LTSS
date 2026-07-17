package com.ltss.dto.content;

import com.ltss.entity.content.ArticleCategoryEntity;

public record ArticleCategoryResponse(Long id, String name, String slug, String description) {
    public static ArticleCategoryResponse from(ArticleCategoryEntity category) {
        return new ArticleCategoryResponse(
                category.getId(), category.getCategoryName(), category.getSlug(), category.getDescription()
        );
    }
}
