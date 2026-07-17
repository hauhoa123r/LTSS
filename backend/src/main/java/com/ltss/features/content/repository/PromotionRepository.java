package com.ltss.features.content.repository;

import com.ltss.features.content.entity.PromotionEntity;
import com.ltss.features.content.entity.PromotionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.time.Instant;
import java.util.Optional;

public interface PromotionRepository extends JpaRepository<PromotionEntity, Long> {
    @Query("""
            select promotion from PromotionEntity promotion
            join BusinessEntity business on business.id = promotion.businessId
            join PlaceEntity place on place.id = business.placeId
            where promotion.status = com.ltss.features.content.entity.PromotionStatus.ACTIVE
              and promotion.startAt <= :now and promotion.endAt > :now
              and business.status = com.ltss.features.content.entity.BusinessStatus.ACTIVE
              and place.status = com.ltss.features.place.entity.PlaceStatus.PUBLISHED
              and (:businessId is null or promotion.businessId = :businessId)
            order by promotion.endAt asc
            """)
    Page<PromotionEntity> findCurrent(
            @Param("businessId") Long businessId,
            @Param("now") Instant now,
            Pageable pageable
    );

    @Query("""
            select promotion from PromotionEntity promotion
            join BusinessEntity business on business.id = promotion.businessId
            join PlaceEntity place on place.id = business.placeId
            where promotion.id = :id
              and promotion.status = com.ltss.features.content.entity.PromotionStatus.ACTIVE
              and promotion.startAt <= :now and promotion.endAt > :now
              and business.status = com.ltss.features.content.entity.BusinessStatus.ACTIVE
              and place.status = com.ltss.features.place.entity.PlaceStatus.PUBLISHED
            """)
    Optional<PromotionEntity> findCurrentById(@Param("id") Long id, @Param("now") Instant now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select promotion from PromotionEntity promotion where promotion.id = :id")
    Optional<PromotionEntity> findLockedById(@Param("id") Long id);
}
