package com.ltss.features.tour.repository;

import com.ltss.features.tour.entity.TourItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TourItemRepository extends JpaRepository<TourItemEntity, Long> {
    List<TourItemEntity> findAllByTourIdOrderByVisitOrderAsc(Long tourId);
    long countByTourId(Long tourId);

    @Modifying
    @Query("delete from TourItemEntity item where item.tourId = :tourId")
    int deleteAllByTourId(@Param("tourId") Long tourId);
}
