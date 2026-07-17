package com.ltss.service.quiz;

import com.ltss.service.quiz.impl.QuizServiceImpl;

import com.ltss.common.exception.ConflictException;
import com.ltss.repository.auth.AuthorizationRepository;
import com.ltss.security.auth.CurrentUserService;
import com.ltss.service.auth.AuditService;
import com.ltss.entity.place.PlaceEntity;
import com.ltss.entity.place.PlaceStatus;
import com.ltss.repository.place.PlaceRepository;
import com.ltss.dto.quiz.*;
import com.ltss.entity.quiz.QuizEntity;
import com.ltss.entity.quiz.QuizStatus;
import com.ltss.repository.quiz.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {
    @Mock QuizRepository quizRepository;
    @Mock QuestionRepository questionRepository;
    @Mock AnswerRepository answerRepository;
    @Mock QuizAttemptRepository attemptRepository;
    @Mock PlaceRepository placeRepository;
    @Mock AuthorizationRepository authorizationRepository;
    @Mock CurrentUserService currentUserService;
    @Mock AuditService auditService;
    QuizService service;

    @BeforeEach
    void setUp() {
        service = new QuizServiceImpl(quizRepository, questionRepository, answerRepository, attemptRepository,
                placeRepository, authorizationRepository, currentUserService, auditService, Clock.systemUTC());
        lenient().when(currentUserService.requireUserId()).thenReturn(10L);
    }

    @Test
    void relicManagerRoleIsRequiredForAuthoring() {
        when(authorizationRepository.findEffectiveRoleCodes(10L)).thenReturn(List.of("TOURIST"));
        assertThrows(AccessDeniedException.class, () -> service.create(validRequest(null), null));
        verify(quizRepository, never()).save(any());
    }

    @Test
    void everyQuestionMustHaveExactlyOneCorrectAnswer() {
        allowManager();
        PlaceEntity place = mock(PlaceEntity.class);
        when(place.getStatus()).thenReturn(PlaceStatus.PUBLISHED);
        when(placeRepository.findById(4L)).thenReturn(Optional.of(place));
        QuestionUpsertRequest invalid = new QuestionUpsertRequest(
                "Câu hỏi", null, BigDecimal.ONE,
                List.of(new AnswerUpsertRequest("A", true), new AnswerUpsertRequest("B", true))
        );
        QuizUpsertRequest request = new QuizUpsertRequest(4L, "Quiz", null, 60,
                new BigDecimal("60"), List.of(invalid), null);

        assertThrows(ConflictException.class, () -> service.create(request, null));
        verify(quizRepository, never()).save(any());
    }

    @Test
    void publishedQuizCannotBeEditedInPlace() {
        allowManager();
        QuizEntity quiz = mock(QuizEntity.class);
        when(quiz.getCreatedByUserId()).thenReturn(10L);
        when(quiz.getStatus()).thenReturn(QuizStatus.PUBLISHED);
        when(quizRepository.findLockedById(7L)).thenReturn(Optional.of(quiz));

        assertThrows(ConflictException.class, () -> service.update(7L, validRequest(0), null));
        verify(questionRepository, never()).deleteAllByQuizId(anyLong());
    }

    private void allowManager() {
        when(authorizationRepository.findEffectiveRoleCodes(10L)).thenReturn(List.of("RELIC_MANAGER"));
    }

    private QuizUpsertRequest validRequest(Integer version) {
        QuestionUpsertRequest question = new QuestionUpsertRequest(
                "Câu hỏi", "Giải thích", BigDecimal.ONE,
                List.of(new AnswerUpsertRequest("Đúng", true), new AnswerUpsertRequest("Sai", false))
        );
        return new QuizUpsertRequest(4L, "Quiz Sơn Tây", null, 60,
                new BigDecimal("60"), List.of(question), version);
    }
}
