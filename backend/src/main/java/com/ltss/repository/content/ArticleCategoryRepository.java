package com.ltss.repository.content;

import com.ltss.entity.content.ArticleCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ArticleCategoryRepository extends JpaRepository<ArticleCategoryEntity, Long> {
    List<ArticleCategoryEntity> findAllByActiveTrueOrderByCategoryNameAsc();
    Page<ArticleCategoryEntity> findAllByOrderByUpdatedAtDesc(Pageable pageable);
    boolean existsByCategoryNameIgnoreCase(String categoryName);
    boolean existsByCategoryNameIgnoreCaseAndIdNot(String categoryName, Long id);
    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, Long id);
}
