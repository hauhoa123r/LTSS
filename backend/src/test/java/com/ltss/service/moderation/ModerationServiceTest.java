package com.ltss.service.moderation;

import com.ltss.service.moderation.impl.ModerationServiceImpl;

import com.ltss.mapper.moderation.ModerationTargetContentMapper;

import com.ltss.common.exception.ConflictException;
import com.ltss.repository.auth.AuthorizationRepository;
import com.ltss.repository.auth.UserRepository;
import com.ltss.security.auth.CurrentUserService;
import com.ltss.service.auth.AuditService;
import com.ltss.service.auth.ClientRequestInfo;
import com.ltss.entity.content.ArticleEntity;
import com.ltss.entity.content.PublicationStatus;
import com.ltss.repository.content.*;
import com.ltss.dto.moderation.ModerationDecisionRequest;
import com.ltss.dto.moderation.SubmitModerationRequest;
import com.ltss.entity.moderation.*;
import com.ltss.repository.moderation.ModerationRepository;
import com.ltss.repository.moderation.NotificationRepository;
import com.ltss.repository.community.ReviewRepository;
import com.ltss.entity.community.ReviewEntity;
import com.ltss.entity.community.ReviewStatus;
import com.ltss.repository.quiz.QuizRepository;
import com.ltss.service.quiz.QuizAggregateValidator;
import com.ltss.service.moderation.strategy.ArticleModerationStrategy;
import com.ltss.service.moderation.strategy.BusinessPostModerationStrategy;
import com.ltss.service.moderation.strategy.EventModerationStrategy;
import com.ltss.service.moderation.strategy.ModerationTargetStrategyRegistry;
import com.ltss.service.moderation.strategy.PromotionModerationStrategy;
import com.ltss.service.moderation.strategy.QuizModerationStrategy;
import com.ltss.service.moderation.strategy.ReviewModerationStrategy;
import com.ltss.entity.quiz.QuizEntity;
import com.ltss.entity.quiz.QuizStatus;
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
    @Mock private ModerationTargetContentMapper targetContentMapper;
    @Mock private AuthorizationRepository authorizationRepository;
    @Mock private UserRepository userRepository;
    @Mock private CurrentUserService currentUserService;
    @Mock private AuditService auditService;

    private ModerationService service;

    @BeforeEach
    void setUp() {
        ModerationTargetStrategyRegistry targetStrategies = new ModerationTargetStrategyRegistry(List.of(
                new ArticleModerationStrategy(articleRepository),
                new EventModerationStrategy(eventRepository),
                new BusinessPostModerationStrategy(postRepository, businessRepository),
                new PromotionModerationStrategy(promotionRepository, businessRepository),
                new QuizModerationStrategy(quizRepository, quizAggregateValidator),
                new ReviewModerationStrategy(reviewRepository)
        ));
        service = new ModerationServiceImpl(
                moderationRepository, notificationRepository, targetStrategies,
                targetContentMapper, authorizationRepository, userRepository,
                currentUserService, auditService, Clock.fixed(NOW, ZoneOffset.UTC)
        );
        lenient().when(currentUserService.requireUserId()).thenReturn(10L);
    }

    @Test
    void ownerCanSubmitDraftAndAllSideEffectsStayInWorkflow() {
        ArticleEntity article = mockArticle(PublicationStatus.DRAFT, 3, 10L);
        when(articleRepository.findLockedById(7L)).thenReturn(Optional.of(article));
        when(moderationRepository.findPending("ARTICLE", 7L)).thenReturn(Optional.empty());
        when(targetContentMapper.snapshot(article)).thenReturn(java.util.Map.of("body", "Nội dung người soạn"));
        when(moderationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(authorizationRepository.findActiveUserIdsWithDirectRoles(any())).thenReturn(List.of(20L));

        service.submit(ModerationTargetType.ARTICLE, 7L, new SubmitModerationRequest(3, "  ready  "), REQUEST_INFO);

        verify(article).submit(NOW);
        ArgumentCaptor<ModerationRecordEntity> record = ArgumentCaptor.forClass(ModerationRecordEntity.class);
        verify(moderationRepository).save(record.capture());
        org.junit.jupiter.api.Assertions.assertEquals("ready", record.getValue().getSubmissionNote());
        org.junit.jupiter.api.Assertions.assertEquals(
                "Nội dung người soạn", record.getValue().getTargetSnapshot().get("body")
        );
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
        when(targetContentMapper.snapshot(article)).thenReturn(java.util.Map.of("body", "Nội dung người soạn"));
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
