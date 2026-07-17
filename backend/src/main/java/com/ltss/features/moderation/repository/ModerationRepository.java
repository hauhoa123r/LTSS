package com.ltss.features.moderation.repository;

import com.ltss.features.moderation.entity.ModerationRecordEntity;
import com.ltss.features.moderation.entity.ModerationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ModerationRepository extends JpaRepository<ModerationRecordEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select record from ModerationRecordEntity record where record.id = :id")
    Optional<ModerationRecordEntity> findLockedById(@Param("id") Long id);

    @Query("""
            select record from ModerationRecordEntity record
            where record.status = com.ltss.features.moderation.entity.ModerationStatus.PENDING
              and ((:type = 'ARTICLE' and record.articleId = :targetId)
                or (:type = 'EVENT' and record.eventId = :targetId)
                or (:type = 'BUSINESS_POST' and record.businessPostId = :targetId)
                or (:type = 'PROMOTION' and record.promotionId = :targetId)
                or (:type = 'QUIZ' and record.quizId = :targetId)
                or (:type = 'REVIEW' and record.reviewId = :targetId))
            """)
    Optional<ModerationRecordEntity> findPending(
            @Param("type") String type,
            @Param("targetId") Long targetId
    );

    @Query("""
            select record from ModerationRecordEntity record
            where record.status = :status
              and ((:type is null and (record.articleId is not null or record.eventId is not null
                    or record.businessPostId is not null or record.promotionId is not null
                    or record.quizId is not null or record.reviewId is not null))
                or (:type = 'ARTICLE' and record.articleId is not null)
                or (:type = 'EVENT' and record.eventId is not null)
                or (:type = 'BUSINESS_POST' and record.businessPostId is not null)
                or (:type = 'PROMOTION' and record.promotionId is not null)
                or (:type = 'QUIZ' and record.quizId is not null)
                or (:type = 'REVIEW' and record.reviewId is not null))
            order by record.submittedAt asc
            """)
    Page<ModerationRecordEntity> queue(
            @Param("status") ModerationStatus status,
            @Param("type") String type,
            Pageable pageable
    );

    @Query("""
            select record from ModerationRecordEntity record
            where (:type = 'ARTICLE' and record.articleId = :targetId)
               or (:type = 'EVENT' and record.eventId = :targetId)
               or (:type = 'BUSINESS_POST' and record.businessPostId = :targetId)
               or (:type = 'PROMOTION' and record.promotionId = :targetId)
               or (:type = 'QUIZ' and record.quizId = :targetId)
               or (:type = 'REVIEW' and record.reviewId = :targetId)
            order by record.submittedAt desc
            """)
    Page<ModerationRecordEntity> history(
            @Param("type") String type,
            @Param("targetId") Long targetId,
            Pageable pageable
    );
}
