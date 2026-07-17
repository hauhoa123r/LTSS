package com.ltss.repository.content;

import com.ltss.entity.content.PromotionEntity;
import com.ltss.entity.content.PromotionStatus;
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
            where promotion.status = com.ltss.entity.content.PromotionStatus.ACTIVE
              and promotion.startAt <= :now and promotion.endAt > :now
              and business.status = com.ltss.entity.content.BusinessStatus.ACTIVE
              and place.status = com.ltss.entity.place.PlaceStatus.PUBLISHED
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
              and promotion.status = com.ltss.entity.content.PromotionStatus.ACTIVE
              and promotion.startAt <= :now and promotion.endAt > :now
              and business.status = com.ltss.entity.content.BusinessStatus.ACTIVE
              and place.status = com.ltss.entity.place.PlaceStatus.PUBLISHED
            """)
    Optional<PromotionEntity> findCurrentById(@Param("id") Long id, @Param("now") Instant now);
    Page<PromotionEntity> findAllByBusinessIdAndStatusNotOrderByUpdatedAtDesc(
            Long businessId, PromotionStatus status, Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select promotion from PromotionEntity promotion where promotion.id = :id")
    Optional<PromotionEntity> findLockedById(@Param("id") Long id);
}
