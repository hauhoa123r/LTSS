package com.ltss.service.quiz;

import com.ltss.service.quiz.impl.QuizAttemptServiceImpl;

import com.ltss.common.exception.ConflictException;
import com.ltss.entity.auth.UserEntity;
import com.ltss.repository.auth.UserRepository;
import com.ltss.security.auth.CurrentUserService;
import com.ltss.service.auth.AuditService;
import com.ltss.service.auth.ClientRequestInfo;
import com.ltss.repository.moderation.NotificationRepository;
import com.ltss.entity.place.PlaceEntity;
import com.ltss.entity.place.PlaceStatus;
import com.ltss.repository.place.PlaceRepository;
import com.ltss.dto.quiz.*;
import com.ltss.entity.quiz.*;
import com.ltss.repository.quiz.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizAttemptServiceTest {
    private static final Instant NOW = Instant.parse("2026-07-16T08:00:00Z");
    private static final ClientRequestInfo REQUEST_INFO = new ClientRequestInfo("127.0.0.1", "req-1");
    @Mock QuizRepository quizRepository;
    @Mock QuestionRepository questionRepository;
    @Mock AnswerRepository answerRepository;
    @Mock QuizAttemptRepository attemptRepository;
    @Mock QuizAttemptAnswerRepository attemptAnswerRepository;
    @Mock QuizBadgeRepository quizBadgeRepository;
    @Mock BadgeRepository badgeRepository;
    @Mock UserBadgeRepository userBadgeRepository;
    @Mock PlaceRepository placeRepository;
    @Mock UserRepository userRepository;
    @Mock NotificationRepository notificationRepository;
    @Mock CurrentUserService currentUserService;
    @Mock QuizAggregateValidator aggregateValidator;
    @Mock AuditService auditService;
    QuizAttemptService service;

    @BeforeEach
    void setUp() {
        service = new QuizAttemptServiceImpl(
                quizRepository, questionRepository, answerRepository, attemptRepository,
                attemptAnswerRepository, quizBadgeRepository, badgeRepository, userBadgeRepository,
                placeRepository, userRepository, notificationRepository, currentUserService,
                aggregateValidator, auditService, Clock.fixed(NOW, ZoneOffset.UTC)
        );
        lenient().when(currentUserService.requireUserId()).thenReturn(10L);
    }

    @Test
    void locationOutsideTwoHundredMetersCannotStartAttempt() {
        QuizEntity quiz = publishedQuiz();
        PlaceEntity place = mock(PlaceEntity.class);
        when(place.getStatus()).thenReturn(PlaceStatus.PUBLISHED);
        when(place.getLatitude()).thenReturn(BigDecimal.ZERO);
        when(place.getLongitude()).thenReturn(new BigDecimal("0.00181"));
        when(quizRepository.findById(7L)).thenReturn(Optional.of(quiz));
        when(placeRepository.findById(4L)).thenReturn(Optional.of(place));

        assertThrows(ConflictException.class, () -> service.start(
                7L, new StartAttemptRequest(BigDecimal.ZERO, BigDecimal.ZERO), REQUEST_INFO
        ));
        verify(attemptRepository, never()).save(any());
    }

    @Test
    void correctSubmissionIsGradedOnceAndSecondSubmitIsIdempotent() {
        QuizEntity quiz = publishedQuiz();
        QuizAttemptEntity attempt = new QuizAttemptEntity(
                7L, 10L, "seed", NOW.minusSeconds(10), NOW.plusSeconds(50), BigDecimal.ONE, BigDecimal.TEN
        );
        ReflectionTestUtils.setField(attempt, "id", 30L);
        QuestionEntity question = new QuestionEntity(7L, "Câu hỏi", "Giải thích", 1, BigDecimal.ONE);
        ReflectionTestUtils.setField(question, "id", 11L);
        AnswerEntity answer = new AnswerEntity(11L, "Đúng", true, 1);
        ReflectionTestUtils.setField(answer, "id", 21L);
        QuizAttemptAnswerEntity row = new QuizAttemptAnswerEntity(30L, 11L, 1, "Câu hỏi", "Đúng", "Giải thích");

        when(attemptRepository.findLockedById(30L)).thenReturn(Optional.of(attempt));
        when(attemptAnswerRepository.findAllByAttemptIdOrderByQuestionOrderAsc(30L)).thenReturn(List.of(row));
        when(answerRepository.findAllById(any())).thenReturn(List.of(answer));
        when(questionRepository.findAllById(any())).thenReturn(List.of(question));
        when(quizRepository.findById(7L)).thenReturn(Optional.of(quiz));
        when(userRepository.findLockedById(10L)).thenReturn(Optional.of(mock(UserEntity.class)));
        when(quizBadgeRepository.findAllByQuizId(7L)).thenReturn(List.of());
        when(userBadgeRepository.findAllByAwardedAttemptIdOrderByAwardedAtAsc(30L)).thenReturn(List.of());

        SubmitAttemptRequest request = new SubmitAttemptRequest(List.of(new AnswerSubmissionRequest(1, 21L)));
        QuizAttemptResponse first = service.submit(30L, request, REQUEST_INFO);
        QuizAttemptResponse second = service.submit(30L, request, REQUEST_INFO);

        assertEquals(QuizAttemptStatus.SUBMITTED, first.status());
        assertEquals(new BigDecimal("100.00"), first.scorePercent());
        assertTrue(first.passed());
        assertEquals(QuizAttemptStatus.SUBMITTED, second.status());
        verify(auditService, times(1)).recordDomain(10L, "QUIZ_ATTEMPT_SUBMITTED", "QUIZ_ATTEMPT", 30L, REQUEST_INFO);
    }

    @Test
    void expiredSubmissionAutoSubmitsWithoutAcceptingLateAnswer() {
        QuizEntity quiz = publishedQuiz();
        QuizAttemptEntity attempt = new QuizAttemptEntity(
                7L, 10L, "seed", NOW.minusSeconds(100), NOW, BigDecimal.ONE, BigDecimal.TEN
        );
        ReflectionTestUtils.setField(attempt, "id", 31L);
        QuestionEntity question = new QuestionEntity(7L, "Câu hỏi", null, 1, BigDecimal.ONE);
        ReflectionTestUtils.setField(question, "id", 11L);
        QuizAttemptAnswerEntity row = new QuizAttemptAnswerEntity(31L, 11L, 1, "Câu hỏi", "Đúng", null);
        when(attemptRepository.findLockedById(31L)).thenReturn(Optional.of(attempt));
        when(attemptAnswerRepository.findAllByAttemptIdOrderByQuestionOrderAsc(31L)).thenReturn(List.of(row));
        when(questionRepository.findAllById(any())).thenReturn(List.of(question));
        when(quizRepository.findById(7L)).thenReturn(Optional.of(quiz));
        when(userRepository.findLockedById(10L)).thenReturn(Optional.of(mock(UserEntity.class)));
        when(quizBadgeRepository.findAllByQuizId(7L)).thenReturn(List.of());
        when(userBadgeRepository.findAllByAwardedAttemptIdOrderByAwardedAtAsc(31L)).thenReturn(List.of());

        QuizAttemptResponse response = service.submit(31L,
                new SubmitAttemptRequest(List.of(new AnswerSubmissionRequest(1, 99L))), REQUEST_INFO);

        assertEquals(QuizAttemptStatus.AUTO_SUBMITTED, response.status());
        assertEquals(new BigDecimal("0.00"), response.scorePercent());
        assertNull(row.getSelectedAnswerId());
    }

    private QuizEntity publishedQuiz() {
        QuizEntity quiz = new QuizEntity(4L, 20L, "Quiz", null, 60, new BigDecimal("60"));
        ReflectionTestUtils.setField(quiz, "id", 7L);
        quiz.submit(NOW.minusSeconds(100));
        quiz.approve(NOW.minusSeconds(90));
        return quiz;
    }
}
