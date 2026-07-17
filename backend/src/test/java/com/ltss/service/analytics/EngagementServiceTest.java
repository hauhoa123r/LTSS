package com.ltss.service.analytics;

import com.ltss.service.analytics.impl.EngagementServiceImpl;

import com.ltss.common.exception.*;
import com.ltss.dto.analytics.*;
import com.ltss.entity.analytics.*;
import com.ltss.repository.analytics.*;
import com.ltss.security.auth.CurrentUserService;
import com.ltss.repository.content.*;
import com.ltss.entity.place.*;
import com.ltss.repository.place.PlaceRepository;
import com.ltss.repository.tour.TourRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EngagementServiceTest {
    @Mock EngagementEventTypeRepository typeRepository;
    @Mock EngagementEventRepository eventRepository;
    @Mock PlaceRepository placeRepository;
    @Mock BusinessRepository businessRepository;
    @Mock EventRepository contentEventRepository;
    @Mock ArticleRepository articleRepository;
    @Mock BusinessPostRepository postRepository;
    @Mock PromotionRepository promotionRepository;
    @Mock TourRepository tourRepository;
    @Mock CurrentUserService currentUserService;
    EngagementService service;

    @BeforeEach
    void setUp() {
        service = new EngagementServiceImpl(typeRepository, eventRepository, placeRepository, businessRepository,
                contentEventRepository, articleRepository, postRepository, promotionRepository, tourRepository,
                currentUserService, Clock.fixed(Instant.parse("2026-07-16T09:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void duplicateViewWithinTwentyFourHoursIsAcceptedWithoutSecondInsert() {
        allowViewAndPlace();
        when(eventRepository.countDuplicate(eq("VIEW"), eq("session-1"), eq("PLACE"), eq(7L), any()))
                .thenReturn(1L);

        EngagementAcceptedResponse response = service.record(request(Map.of()));

        assertFalse(response.recorded());
        verify(eventRepository, never()).save(any());
    }

    @Test
    void sessionRateLimitIsEnforcedBeforeInsert() {
        allowViewAndPlace();
        when(eventRepository.countBySessionKeyAndOccurredAtGreaterThanEqual(eq("session-1"), any()))
                .thenReturn(120L);

        assertThrows(RateLimitException.class, () -> service.record(request(Map.of())));
        verify(eventRepository, never()).save(any());
    }

    @Test
    void sensitiveMetadataKeysAreRejected() {
        allowViewAndPlace();
        assertThrows(BusinessRuleViolationException.class,
                () -> service.record(request(Map.of("accessToken", "secret"))));
        verify(eventRepository, never()).save(any());
    }

    private EngagementEventRequest request(Map<String, String> metadata) {
        return new EngagementEventRequest("VIEW", "session-1", EngagementTargetType.PLACE, 7L, metadata);
    }

    private void allowViewAndPlace() {
        EngagementEventTypeEntity type = mock(EngagementEventTypeEntity.class);
        when(type.isActive()).thenReturn(true);
        when(typeRepository.findLockedByCode("VIEW")).thenReturn(Optional.of(type));
        PlaceEntity place = mock(PlaceEntity.class);
        when(place.getStatus()).thenReturn(PlaceStatus.PUBLISHED);
        when(placeRepository.findById(7L)).thenReturn(Optional.of(place));
    }
}
