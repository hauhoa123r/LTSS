package com.ltss.repository.quiz;

import com.ltss.entity.quiz.QuizBadgeEntity;
import com.ltss.entity.quiz.QuizBadgeId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizBadgeRepository extends JpaRepository<QuizBadgeEntity, QuizBadgeId> {
    List<QuizBadgeEntity> findAllByQuizId(Long quizId);
}
