package com.ltss.features.content.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
}
