package com.ltss.features.quiz.repository;

import com.ltss.features.quiz.entity.QuizBadgeEntity;
import com.ltss.features.quiz.entity.QuizBadgeId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizBadgeRepository extends JpaRepository<QuizBadgeEntity, QuizBadgeId> {
    List<QuizBadgeEntity> findAllByQuizId(Long quizId);
}
