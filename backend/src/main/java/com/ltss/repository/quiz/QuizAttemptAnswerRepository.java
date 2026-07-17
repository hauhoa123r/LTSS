package com.ltss.repository.quiz;

import com.ltss.entity.quiz.QuizAttemptAnswerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizAttemptAnswerRepository extends JpaRepository<QuizAttemptAnswerEntity, Long> {
    List<QuizAttemptAnswerEntity> findAllByAttemptIdOrderByQuestionOrderAsc(Long attemptId);
}
