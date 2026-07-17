package com.ltss.features.place.repository;

import com.ltss.features.place.entity.PlaceCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlaceCategoryRepository extends JpaRepository<PlaceCategoryEntity, Long> {
    List<PlaceCategoryEntity> findAllByActiveTrueOrderByCategoryNameAsc();
}
