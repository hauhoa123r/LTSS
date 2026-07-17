package com.ltss.repository.quiz;

import com.ltss.entity.quiz.QuestionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestionRepository extends JpaRepository<QuestionEntity, Long> {
    List<QuestionEntity> findAllByQuizIdAndActiveTrueOrderByDisplayOrderAsc(Long quizId);

    @Modifying(flushAutomatically = true)
    @Query("delete from QuestionEntity question where question.quizId = :quizId")
    void deleteAllByQuizId(@Param("quizId") Long quizId);
}
