package com.ltss.service.community;

import com.ltss.service.community.impl.ReviewServiceImpl;

import com.ltss.common.exception.ConflictException;
import com.ltss.repository.auth.UserRepository;
import com.ltss.security.auth.CurrentUserService;
import com.ltss.service.auth.AuditService;
import com.ltss.dto.community.*;
import com.ltss.entity.community.*;
import com.ltss.repository.community.*;
import com.ltss.repository.content.*;
import com.ltss.repository.moderation.NotificationRepository;
import com.ltss.service.moderation.ModerationService;
import com.ltss.entity.place.*;
import com.ltss.repository.place.PlaceRepository;
import com.ltss.repository.tour.TourRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {
    @Mock ReviewRepository reviewRepository;
    @Mock ReviewReplyRepository replyRepository;
    @Mock ReviewMediaRepository mediaRepository;
    @Mock ProhibitedTermRepository prohibitedTermRepository;
    @Mock PlaceRepository placeRepository;
    @Mock BusinessRepository businessRepository;
    @Mock ArticleRepository articleRepository;
    @Mock TourRepository tourRepository;
    @Mock UserRepository userRepository;
    @Mock NotificationRepository notificationRepository;
    @Mock CurrentUserService currentUserService;
    @Mock ModerationService moderationService;
    @Mock AuditService auditService;
    ReviewService service;

    @BeforeEach
    void setUp() {
        service = new ReviewServiceImpl(reviewRepository, replyRepository, mediaRepository,
                prohibitedTermRepository, placeRepository, businessRepository, articleRepository,
                tourRepository, userRepository, notificationRepository, currentUserService,
                moderationService, auditService, Clock.fixed(Instant.parse("2026-07-16T06:00:00Z"), ZoneOffset.UTC));
        lenient().when(currentUserService.requireUserId()).thenReturn(10L);
    }

    @Test
    void oneReviewPerUserAndTarget() {
        publicPlace(4L);
        when(reviewRepository.findExisting(10L, "PLACE", 4L)).thenReturn(Optional.of(mock(ReviewEntity.class)));
        assertThrows(ConflictException.class, () -> service.create(ReviewTargetType.PLACE, 4L,
                new CreateReviewRequest(5, "Một trải nghiệm thực sự rất đáng nhớ", List.of()), null));
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void blockedTermStopsReviewBeforePersistence() {
        publicPlace(4L);
        ProhibitedTermEntity term = mock(ProhibitedTermEntity.class);
        when(term.getNormalizedTerm()).thenReturn("xau xi");
        when(prohibitedTermRepository.findAllByActiveTrueAndSeverityOrderByIdAsc("BLOCK")).thenReturn(List.of(term));
        when(reviewRepository.findExisting(10L, "PLACE", 4L)).thenReturn(Optional.empty());
        assertThrows(ConflictException.class, () -> service.create(ReviewTargetType.PLACE, 4L,
                new CreateReviewRequest(4, "Trải nghiệm này thật xấu xí và không phù hợp", List.of()), null));
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void createReviewRegistersPendingModerationCase() {
        publicPlace(4L);
        when(reviewRepository.findExisting(10L, "PLACE", 4L)).thenReturn(Optional.empty());
        when(prohibitedTermRepository.findAllByActiveTrueAndSeverityOrderByIdAsc("BLOCK")).thenReturn(List.of());
        ReviewEntity saved = mock(ReviewEntity.class);
        when(saved.getId()).thenReturn(7L);
        when(saved.getUserId()).thenReturn(10L);
        when(saved.targetType()).thenReturn(ReviewTargetType.PLACE);
        when(saved.targetId()).thenReturn(4L);
        when(saved.getStatus()).thenReturn(ReviewStatus.PENDING);
        when(reviewRepository.save(any())).thenReturn(saved);
        when(replyRepository.findAllByReviewIdIn(any())).thenReturn(List.of());
        when(mediaRepository.findMedia(any())).thenReturn(List.of());
        when(userRepository.findAllById(any())).thenReturn(List.of());

        service.create(ReviewTargetType.PLACE, 4L,
                new CreateReviewRequest(5, "Một trải nghiệm thực sự rất đáng nhớ", List.of()), null);

        verify(moderationService).registerPendingReview(7L, 10L, null);
    }

    @Test
    void onlyReviewedBusinessOwnerCanReply() {
        ReviewEntity review = mock(ReviewEntity.class);
        when(review.getStatus()).thenReturn(ReviewStatus.VISIBLE);
        when(review.getBusinessId()).thenReturn(3L);
        when(reviewRepository.findLockedById(7L)).thenReturn(Optional.of(review));
        com.ltss.entity.content.BusinessEntity business = mock(com.ltss.entity.content.BusinessEntity.class);
        when(business.getOwnerUserId()).thenReturn(20L);
        when(businessRepository.findById(3L)).thenReturn(Optional.of(business));

        assertThrows(AccessDeniedException.class, () -> service.reply(7L,
                new ReviewReplyRequest("Cảm ơn bạn đã chia sẻ trải nghiệm"), null));
        verify(replyRepository, never()).save(any());
    }

    private void publicPlace(Long id) {
        PlaceEntity place = mock(PlaceEntity.class);
        when(place.getStatus()).thenReturn(PlaceStatus.PUBLISHED);
        when(placeRepository.findById(id)).thenReturn(Optional.of(place));
    }
}
