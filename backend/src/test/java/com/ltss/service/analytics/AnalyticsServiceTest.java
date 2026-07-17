package com.ltss.service.analytics;

import com.ltss.service.analytics.impl.AnalyticsServiceImpl;

import com.ltss.common.exception.BusinessRuleViolationException;
import com.ltss.dto.analytics.AnalyticsOverviewResponse;
import com.ltss.repository.analytics.EngagementEventRepository;
import com.ltss.repository.auth.*;
import com.ltss.security.auth.CurrentUserService;
import com.ltss.entity.content.*;
import com.ltss.repository.content.BusinessRepository;
import com.ltss.entity.place.PlaceEntity;
import com.ltss.repository.place.PlaceRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {
    @Mock EngagementEventRepository eventRepository;
    @Mock BusinessRepository businessRepository;
    @Mock PlaceRepository placeRepository;
    @Mock UserRepository userRepository;
    @Mock AuditLogRepository auditLogRepository;
    @Mock AuthorizationRepository authorizationRepository;
    @Mock CurrentUserService currentUserService;
    AnalyticsService service;

    @BeforeEach
    void setUp() {
        service = new AnalyticsServiceImpl(eventRepository, businessRepository, placeRepository, userRepository,
                auditLogRepository, authorizationRepository, currentUserService);
        lenient().when(currentUserService.requireUserId()).thenReturn(10L);
    }

    @Test
    void systemAnalyticsRequireAdministratorRole() {
        when(authorizationRepository.findEffectiveRoleCodes(10L)).thenReturn(List.of("MODERATOR"));
        assertThrows(AccessDeniedException.class,
                () -> service.system(LocalDate.parse("2026-07-01"), LocalDate.parse("2026-07-16")));
    }

    @Test
    void invalidDateRangeIsRejected() {
        when(authorizationRepository.findEffectiveRoleCodes(10L)).thenReturn(List.of("ADMINISTRATOR"));
        assertThrows(BusinessRuleViolationException.class,
                () -> service.system(LocalDate.parse("2026-07-17"), LocalDate.parse("2026-07-16")));
    }

    @Test
    void businessAnalyticsNeverAcceptArbitraryBusinessId() {
        BusinessEntity business = mock(BusinessEntity.class);
        when(business.getStatus()).thenReturn(BusinessStatus.ACTIVE);
        when(business.getId()).thenReturn(9L);
        when(business.getPlaceId()).thenReturn(4L);
        when(businessRepository.findByOwnerUserId(10L)).thenReturn(Optional.of(business));
        PlaceEntity place = mock(PlaceEntity.class);
        when(place.getName()).thenReturn("Làng cổ");
        when(placeRepository.findById(4L)).thenReturn(Optional.of(place));

        AnalyticsOverviewResponse response = service.ownBusiness(
                LocalDate.parse("2026-07-01"), LocalDate.parse("2026-07-16"));

        assertEquals(9L, response.businessId());
        verify(eventRepository).countBusinessEvents(eq(9L), eq(4L), any(), any());
    }
}
