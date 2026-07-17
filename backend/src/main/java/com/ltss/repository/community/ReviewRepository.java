package com.ltss.repository.community;

import com.ltss.entity.community.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<ReviewEntity, Long> {
    @Query("""
            select review from ReviewEntity review
            where review.status = com.ltss.entity.community.ReviewStatus.VISIBLE
              and ((:type = 'PLACE' and review.placeId = :targetId)
                or (:type = 'BUSINESS' and review.businessId = :targetId)
                or (:type = 'ARTICLE' and review.articleId = :targetId)
                or (:type = 'TOUR' and review.tourId = :targetId))
            order by review.publishedAt desc
            """)
    Page<ReviewEntity> findVisible(@Param("type") String type, @Param("targetId") Long targetId, Pageable pageable);

    @Query("""
            select review from ReviewEntity review
            where review.userId = :userId and ((:type = 'PLACE' and review.placeId = :targetId)
                or (:type = 'BUSINESS' and review.businessId = :targetId)
                or (:type = 'ARTICLE' and review.articleId = :targetId)
                or (:type = 'TOUR' and review.tourId = :targetId))
            """)
    Optional<ReviewEntity> findExisting(@Param("userId") Long userId, @Param("type") String type,
                                        @Param("targetId") Long targetId);

    Page<ReviewEntity> findAllByUserIdOrderBySubmittedAtDesc(Long userId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select review from ReviewEntity review where review.id = :id")
    Optional<ReviewEntity> findLockedById(@Param("id") Long id);
}
