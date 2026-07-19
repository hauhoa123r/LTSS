package com.ltss.repository.place;

import com.ltss.entity.place.PlaceCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlaceCategoryRepository extends JpaRepository<PlaceCategoryEntity, Long> {
    List<PlaceCategoryEntity> findAllByActiveTrueOrderByCategoryNameAsc();
    org.springframework.data.domain.Page<PlaceCategoryEntity> findAllByOrderByUpdatedAtDesc(org.springframework.data.domain.Pageable pageable);
    boolean existsByCategoryNameIgnoreCase(String categoryName);
    boolean existsByCategoryNameIgnoreCaseAndIdNot(String categoryName, Long id);
    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, Long id);
}
