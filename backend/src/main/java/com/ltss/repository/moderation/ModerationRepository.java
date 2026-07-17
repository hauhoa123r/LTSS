package com.ltss.repository.moderation;

import com.ltss.entity.moderation.ModerationRecordEntity;
import com.ltss.entity.moderation.ModerationStatus;
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
            where record.status = com.ltss.entity.moderation.ModerationStatus.PENDING
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
              and (((:type is null or :type = 'ARTICLE') and record.articleId is not null
                    and exists (select article.id from ArticleEntity article
                                where article.id = record.articleId
                                  and article.status = com.ltss.entity.content.PublicationStatus.PENDING))
                or ((:type is null or :type = 'EVENT') and record.eventId is not null
                    and exists (select event.id from EventEntity event
                                where event.id = record.eventId
                                  and event.status = com.ltss.entity.content.EventStatus.PENDING))
                or ((:type is null or :type = 'BUSINESS_POST') and record.businessPostId is not null
                    and exists (select post.id from BusinessPostEntity post
                                where post.id = record.businessPostId
                                  and post.status = com.ltss.entity.content.PublicationStatus.PENDING))
                or ((:type is null or :type = 'PROMOTION') and record.promotionId is not null
                    and exists (select promotion.id from PromotionEntity promotion
                                where promotion.id = record.promotionId
                                  and promotion.status = com.ltss.entity.content.PromotionStatus.PENDING))
                or ((:type is null or :type = 'QUIZ') and record.quizId is not null
                    and exists (select quiz.id from QuizEntity quiz
                                where quiz.id = record.quizId
                                  and quiz.status = com.ltss.entity.quiz.QuizStatus.PENDING))
                or ((:type is null or :type = 'REVIEW') and record.reviewId is not null
                    and exists (select review.id from ReviewEntity review
                                where review.id = record.reviewId
                                  and review.status = com.ltss.entity.community.ReviewStatus.PENDING)))
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
