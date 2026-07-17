package com.ltss.features.quiz.repository;

import com.ltss.features.quiz.entity.QuizAttemptAnswerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizAttemptAnswerRepository extends JpaRepository<QuizAttemptAnswerEntity, Long> {
    List<QuizAttemptAnswerEntity> findAllByAttemptIdOrderByQuestionOrderAsc(Long attemptId);
}
