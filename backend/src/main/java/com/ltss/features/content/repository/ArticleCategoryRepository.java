package com.ltss.features.content.repository;

import com.ltss.features.content.entity.ArticleCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArticleCategoryRepository extends JpaRepository<ArticleCategoryEntity, Long> {
    List<ArticleCategoryEntity> findAllByActiveTrueOrderByCategoryNameAsc();
}
