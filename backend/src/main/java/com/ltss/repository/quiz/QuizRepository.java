package com.ltss.repository.quiz;

import com.ltss.entity.quiz.QuizEntity;
import com.ltss.entity.quiz.QuizStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface QuizRepository extends JpaRepository<QuizEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select quiz from QuizEntity quiz where quiz.id = :id")
    Optional<QuizEntity> findLockedById(@Param("id") Long id);

    @Query("""
            select quiz from QuizEntity quiz
            where quiz.status = com.ltss.entity.quiz.QuizStatus.PUBLISHED
              and (:placeId is null or quiz.placeId = :placeId)
            order by quiz.publishedAt desc, quiz.id desc
            """)
    Page<QuizEntity> findPublished(@Param("placeId") Long placeId, Pageable pageable);

    Page<QuizEntity> findAllByCreatedByUserIdAndStatusNotOrderByUpdatedAtDesc(
            Long userId, QuizStatus status, Pageable pageable
    );
}
