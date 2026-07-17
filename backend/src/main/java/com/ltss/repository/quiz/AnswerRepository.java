package com.ltss.repository.quiz;

import com.ltss.entity.quiz.AnswerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface AnswerRepository extends JpaRepository<AnswerEntity, Long> {
    List<AnswerEntity> findAllByQuestionIdInAndActiveTrueOrderByQuestionIdAscDisplayOrderAsc(Collection<Long> questionIds);
}
