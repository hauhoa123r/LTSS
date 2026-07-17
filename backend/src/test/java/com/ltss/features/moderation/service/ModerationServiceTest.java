package com.ltss.features.moderation.service;

import com.ltss.common.exception.ConflictException;
import com.ltss.features.auth.repository.AuthorizationRepository;
import com.ltss.features.auth.security.CurrentUserService;
import com.ltss.features.auth.service.AuditService;
import com.ltss.features.auth.service.ClientRequestInfo;
import com.ltss.features.content.entity.ArticleEntity;
import com.ltss.features.content.entity.PublicationStatus;
import com.ltss.features.content.repository.*;
import com.ltss.features.moderation.dto.ModerationDecisionRequest;
import com.ltss.features.moderation.dto.SubmitModerationRequest;
import com.ltss.features.moderation.entity.*;
import com.ltss.features.moderation.repository.ModerationRepository;
import com.ltss.features.moderation.repository.NotificationRepository;
import com.ltss.features.community.repository.ReviewRepository;
import com.ltss.features.community.entity.ReviewEntity;
import com.ltss.features.community.entity.ReviewStatus;
import com.ltss.features.quiz.repository.QuizRepository;
import com.ltss.features.quiz.service.QuizAggregateValidator;
import com.ltss.features.quiz.entity.QuizEntity;
import com.ltss.features.quiz.entity.QuizStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModerationServiceTest {
    private static final Instant NOW = Instant.parse("2026-07-16T06:00:00Z");
    private static final ClientRequestInfo REQUEST_INFO = new ClientRequestInfo("127.0.0.1", "request-1");

    @Mock private ModerationRepository moderationRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private ArticleRepository articleRepository;
    @Mock private EventRepository eventRepository;
    @Mock private BusinessPostRepository postRepository;
    @Mock private PromotionRepository promotionRepository;
    @Mock private BusinessRepository businessRepository;
    @Mock private ReviewRepository reviewRepository;
    @Mock private QuizRepository quizRepository;
    @Mock private QuizAggregateValidator quizAggregateValidator;
    @Mock private AuthorizationRepository authorizationRepository;
    @Mock private CurrentUserService currentUserService;
    @Mock private AuditService auditService;

    private ModerationService service;

    @BeforeEach
    void setUp() {
        service = new ModerationService(
                moderationRepository, notificationRepository, articleRepository, eventRepository,
                postRepository, promotionRepository, businessRepository, reviewRepository,
                quizRepository, quizAggregateValidator, authorizationRepository,
                currentUserService, auditService, Clock.fixed(NOW, ZoneOffset.UTC)
        );
        lenient().when(currentUserService.requireUserId()).thenReturn(10L);
    }

    @Test
    void ownerCanSubmitDraftAndAllSideEffectsStayInWorkflow() {
        ArticleEntity article = mockArticle(PublicationStatus.DRAFT, 3, 10L);
        when(articleRepository.findLockedById(7L)).thenReturn(Optional.of(article));
        when(moderationRepository.findPending("ARTICLE", 7L)).thenReturn(Optional.empty());
        when(moderationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(authorizationRepository.findActiveUserIdsWithDirectRoles(any())).thenReturn(List.of(20L));

        service.submit(ModerationTargetType.ARTICLE, 7L, new SubmitModerationRequest(3, "  ready  "), REQUEST_INFO);

        verify(article).submit(NOW);
        ArgumentCaptor<ModerationRecordEntity> record = ArgumentCaptor.forClass(ModerationRecordEntity.class);
        verify(moderationRepository).save(record.capture());
        org.junit.jupiter.api.Assertions.assertEquals("ready", record.getValue().getSubmissionNote());
        verify(notificationRepository).saveAll(anyList());
        verify(auditService).recordDomain(10L, "MODERATION_SUBMITTED", "ARTICLE", 7L, REQUEST_INFO);
    }

    @Test
    void staleVersionStopsSubmissionBeforeMutation() {
        ArticleEntity article = mockArticle(PublicationStatus.DRAFT, 4, 10L);
        when(articleRepository.findLockedById(7L)).thenReturn(Optional.of(article));

        assertThrows(ConflictException.class, () -> service.submit(
                ModerationTargetType.ARTICLE, 7L, new SubmitModerationRequest(3, null), REQUEST_INFO
        ));

        verify(article, never()).submit(any());
        verify(moderationRepository, never()).save(any());
    }

    @Test
    void moderatorApprovalResolvesCaseAndPublishesTarget() {
        ModerationRecordEntity record = pendingCase();
        ArticleEntity article = mockArticle(PublicationStatus.PENDING, 4, 10L);
        when(authorizationRepository.findEffectiveRoleCodes(10L)).thenReturn(List.of("MODERATOR"));
        when(moderationRepository.findLockedById(30L)).thenReturn(Optional.of(record));
        when(articleRepository.findLockedById(7L)).thenReturn(Optional.of(article));

        service.approve(30L, new ModerationDecisionRequest(4, "ok"), REQUEST_INFO);

        verify(article).approve(NOW);
        verify(record).resolve(10L, ModerationDecision.APPROVED, "ok", NOW);
        verify(notificationRepository).save(any(NotificationEntity.class));
        verify(auditService).recordDomain(10L, "MODERATION_APPROVED", "ARTICLE", 7L, REQUEST_INFO);
    }

    @Test
    void nonModeratorCannotResolveCase() {
        when(authorizationRepository.findEffectiveRoleCodes(10L)).thenReturn(List.of("TOURIST"));

        assertThrows(AccessDeniedException.class, () -> service.approve(
                30L, new ModerationDecisionRequest(4, null), REQUEST_INFO
        ));

        verify(moderationRepository, never()).findLockedById(anyLong());
    }

    @Test
    void rejectionRequiresReason() {
        assertThrows(ConflictException.class, () -> service.reject(
                30L, new ModerationDecisionRequest(4, "  "), REQUEST_INFO
        ));
    }

    @Test
    void terminalCaseCannotBeResolvedTwice() {
        ModerationRecordEntity record = mock(ModerationRecordEntity.class);
        when(record.getStatus()).thenReturn(ModerationStatus.RESOLVED);
        when(authorizationRepository.findEffectiveRoleCodes(10L)).thenReturn(List.of("MODERATOR"));
        when(moderationRepository.findLockedById(30L)).thenReturn(Optional.of(record));

        assertThrows(ConflictException.class, () -> service.approve(
                30L, new ModerationDecisionRequest(4, null), REQUEST_INFO
        ));
    }

    @Test
    void downstreamAuditFailurePropagatesSoTransactionCanRollback() {
        ArticleEntity article = mockArticle(PublicationStatus.DRAFT, 3, 10L);
        when(articleRepository.findLockedById(7L)).thenReturn(Optional.of(article));
        when(moderationRepository.findPending("ARTICLE", 7L)).thenReturn(Optional.empty());
        when(moderationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(authorizationRepository.findActiveUserIdsWithDirectRoles(any())).thenReturn(List.of());
        doThrow(new IllegalStateException("audit unavailable"))
                .when(auditService).recordDomain(10L, "MODERATION_SUBMITTED", "ARTICLE", 7L, REQUEST_INFO);

        assertThrows(IllegalStateException.class, () -> service.submit(
                ModerationTargetType.ARTICLE, 7L, new SubmitModerationRequest(3, null), REQUEST_INFO
        ));
    }

    @Test
    void moderatorApprovalMakesReviewVisible() {
        ModerationRecordEntity record = mock(ModerationRecordEntity.class);
        when(record.getStatus()).thenReturn(ModerationStatus.PENDING);
        when(record.targetType()).thenReturn(ModerationTargetType.REVIEW);
        when(record.targetId()).thenReturn(44L);
        when(record.getSubmittedByUserId()).thenReturn(11L);
        ReviewEntity review = mock(ReviewEntity.class);
        when(review.getStatus()).thenReturn(ReviewStatus.PENDING);
        when(review.getVersion()).thenReturn(0);
        when(review.getId()).thenReturn(44L);
        when(authorizationRepository.findEffectiveRoleCodes(10L)).thenReturn(List.of("MODERATOR"));
        when(moderationRepository.findLockedById(31L)).thenReturn(Optional.of(record));
        when(reviewRepository.findLockedById(44L)).thenReturn(Optional.of(review));

        service.approve(31L, new ModerationDecisionRequest(0, null), REQUEST_INFO);

        verify(review).approve(NOW);
        verify(record).resolve(10L, ModerationDecision.APPROVED, null, NOW);
    }

    @Test
    void moderatorApprovalValidatesAndPublishesQuiz() {
        ModerationRecordEntity record = mock(ModerationRecordEntity.class);
        when(record.getStatus()).thenReturn(ModerationStatus.PENDING);
        when(record.targetType()).thenReturn(ModerationTargetType.QUIZ);
        when(record.targetId()).thenReturn(50L);
        when(record.getSubmittedByUserId()).thenReturn(12L);
        QuizEntity quiz = mock(QuizEntity.class);
        when(quiz.getStatus()).thenReturn(QuizStatus.PENDING);
        when(quiz.getVersion()).thenReturn(2);
        when(quiz.getTitle()).thenReturn("Quiz Thành cổ");
        when(authorizationRepository.findEffectiveRoleCodes(10L)).thenReturn(List.of("MODERATOR"));
        when(moderationRepository.findLockedById(32L)).thenReturn(Optional.of(record));
        when(quizRepository.findLockedById(50L)).thenReturn(Optional.of(quiz));

        service.approve(32L, new ModerationDecisionRequest(2, null), REQUEST_INFO);

        verify(quizAggregateValidator).validate(quiz);
        verify(quiz).approve(NOW);
        verify(record).resolve(10L, ModerationDecision.APPROVED, null, NOW);
    }

    private ArticleEntity mockArticle(PublicationStatus status, int version, Long ownerId) {
        ArticleEntity article = mock(ArticleEntity.class);
        lenient().when(article.getStatus()).thenReturn(status);
        lenient().when(article.getVersion()).thenReturn(version);
        lenient().when(article.getAuthorUserId()).thenReturn(ownerId);
        lenient().when(article.getTitle()).thenReturn("Di sản Sơn Tây");
        return article;
    }

    private ModerationRecordEntity pendingCase() {
        ModerationRecordEntity record = mock(ModerationRecordEntity.class);
        when(record.getStatus()).thenReturn(ModerationStatus.PENDING);
        when(record.targetType()).thenReturn(ModerationTargetType.ARTICLE);
        when(record.targetId()).thenReturn(7L);
        when(record.getSubmittedByUserId()).thenReturn(10L);
        return record;
    }
}
