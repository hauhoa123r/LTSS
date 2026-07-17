package com.ltss.service.quiz;

import com.ltss.common.exception.ConflictException;
import com.ltss.entity.quiz.AnswerEntity;
import com.ltss.entity.quiz.QuestionEntity;
import com.ltss.entity.quiz.QuizEntity;
import com.ltss.repository.quiz.AnswerRepository;
import com.ltss.repository.quiz.QuestionRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class QuizAggregateValidator {
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;

    public QuizAggregateValidator(QuestionRepository questionRepository, AnswerRepository answerRepository) {
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
    }

    public void validate(QuizEntity quiz) {
        List<QuestionEntity> questions = questionRepository
                .findAllByQuizIdAndActiveTrueOrderByDisplayOrderAsc(quiz.getId());
        if (questions.isEmpty()) throw new ConflictException("Quiz must contain at least one active question");
        Map<Long, List<AnswerEntity>> answers = answerRepository
                .findAllByQuestionIdInAndActiveTrueOrderByQuestionIdAscDisplayOrderAsc(
                        questions.stream().map(QuestionEntity::getId).toList()
                ).stream().collect(Collectors.groupingBy(AnswerEntity::getQuestionId));
        for (QuestionEntity question : questions) {
            List<AnswerEntity> choices = answers.getOrDefault(question.getId(), List.of());
            if (choices.size() < 2 || choices.size() > 4) {
                throw new ConflictException("Every quiz question must have two to four active answers");
            }
            if (choices.stream().filter(AnswerEntity::isCorrect).count() != 1) {
                throw new ConflictException("Every quiz question must have exactly one correct answer");
            }
        }
    }
}
