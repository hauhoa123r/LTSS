package com.ltss.repository.place;

import com.ltss.entity.place.PlaceCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlaceCategoryRepository extends JpaRepository<PlaceCategoryEntity, Long> {
    List<PlaceCategoryEntity> findAllByActiveTrueOrderByCategoryNameAsc();
}
