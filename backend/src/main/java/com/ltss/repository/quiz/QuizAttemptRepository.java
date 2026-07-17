package com.ltss.repository.quiz;

import com.ltss.entity.quiz.QuizAttemptEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface QuizAttemptRepository extends JpaRepository<QuizAttemptEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select attempt from QuizAttemptEntity attempt where attempt.id = :id")
    Optional<QuizAttemptEntity> findLockedById(@Param("id") Long id);

    Page<QuizAttemptEntity> findAllByUserIdOrderByStartedAtDesc(Long userId, Pageable pageable);
    boolean existsByQuizId(Long quizId);
}
