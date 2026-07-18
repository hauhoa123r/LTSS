package com.ltss.service.analytics;

import com.ltss.service.analytics.impl.AnalyticsServiceImpl;

import com.ltss.common.exception.BusinessRuleViolationException;
import com.ltss.dto.analytics.AnalyticsOverviewResponse;
import com.ltss.dto.analytics.BusinessStatisticsResponse;
import com.ltss.dto.analytics.MonumentGranularity;
import com.ltss.dto.analytics.MonumentStatisticsResponse;
import com.ltss.dto.analytics.MonthlyEventStatisticsResponse;
import com.ltss.repository.analytics.EngagementEventRepository;
import com.ltss.repository.analytics.MetricCountProjection;
import com.ltss.repository.analytics.MonumentVisitProjection;
import com.ltss.repository.auth.*;
import com.ltss.security.auth.CurrentUserService;
import com.ltss.entity.content.*;
import com.ltss.repository.content.BusinessRepository;
import com.ltss.entity.place.PlaceEntity;
import com.ltss.repository.content.BusinessAccountStatisticsProjection;
import com.ltss.repository.content.BusinessCategoryStatisticsProjection;
import com.ltss.repository.content.EventRepository;
import com.ltss.repository.content.MonthlyEventStatisticsProjection;
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
    @Mock EventRepository contentEventRepository;
    @Mock PlaceRepository placeRepository;
    @Mock UserRepository userRepository;
    @Mock AuditLogRepository auditLogRepository;
    @Mock AuthorizationRepository authorizationRepository;
    @Mock CurrentUserService currentUserService;
    AnalyticsService service;

    @BeforeEach
    void setUp() {
        service = new AnalyticsServiceImpl(eventRepository, businessRepository, contentEventRepository, placeRepository, userRepository,
                auditLogRepository, authorizationRepository, currentUserService,
                Clock.fixed(Instant.parse("2026-07-16T09:00:00Z"), ZoneOffset.UTC));
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

    @Test
    void monumentStatisticsDefaultsToLastThirtyDaysAndAggregatesPlaceViews() {
        when(authorizationRepository.findEffectiveRoleCodes(10L)).thenReturn(List.of("ADMINISTRATOR"));
        when(eventRepository.countPlaceViews(any(), any())).thenReturn(12L);
        when(eventRepository.countPlaceViewSessions(any(), any())).thenReturn(8L);
        when(eventRepository.countPlaceViewUsers(any(), any())).thenReturn(3L);
        when(eventRepository.countPlaceViewsDaily(any(), any())).thenReturn(List.of());
        when(eventRepository.countPlaceViewsByMonument(any(), any())).thenReturn(List.of());
        when(eventRepository.countPlaceViewsDailyByMonument(any(), any())).thenReturn(List.of());
        MonumentVisitProjection monument = mock(MonumentVisitProjection.class);
        when(monument.getPlaceId()).thenReturn(4L);
        when(monument.getName()).thenReturn("Thành cổ Sơn Tây");
        when(monument.getSlug()).thenReturn("thanh-co-son-tay");
        when(monument.getVisits()).thenReturn(12L);
        when(monument.getUniqueSessions()).thenReturn(8L);
        when(monument.getAuthenticatedVisitors()).thenReturn(3L);
        when(eventRepository.countVisitsByMonument(any(), any())).thenReturn(List.of(monument));

        MonumentStatisticsResponse response = service.monumentStatistics(null, null, MonumentGranularity.DAILY);

        assertEquals(LocalDate.parse("2026-06-17"), response.startDate());
        assertEquals(LocalDate.parse("2026-07-16"), response.endDate());
        assertEquals(MonumentGranularity.DAILY, response.granularity());
        assertEquals(12L, response.totalVisits());
        assertEquals(0.4, response.averageVisitsPerDay());
        assertEquals(1, response.monuments().size());
        assertEquals("Thành cổ Sơn Tây", response.monuments().getFirst().name());
        verify(eventRepository).countPlaceViews(
                eq(Instant.parse("2026-06-17T00:00:00Z")),
                eq(Instant.parse("2026-07-17T00:00:00Z")));
    }

    @Test
    void businessStatisticsAggregatesAccountsWithoutSensitiveFields() {
        when(authorizationRepository.findEffectiveRoleCodes(10L)).thenReturn(List.of("ADMINISTRATOR"));
        when(businessRepository.countByStatus(BusinessStatus.ACTIVE)).thenReturn(6L);
        when(businessRepository.countByStatus(BusinessStatus.PENDING)).thenReturn(2L);
        when(businessRepository.countByStatus(BusinessStatus.SUSPENDED)).thenReturn(1L);
        when(businessRepository.countByStatus(BusinessStatus.INACTIVE)).thenReturn(1L);
        when(businessRepository.countByStatus(BusinessStatus.REJECTED)).thenReturn(1L);
        when(businessRepository.countCreatedInRange(
                eq(Instant.parse("2026-06-17T00:00:00Z")),
                eq(Instant.parse("2026-07-17T00:00:00Z")))).thenReturn(4L);
        when(businessRepository.countCreatedInRange(
                eq(Instant.parse("2026-05-18T00:00:00Z")),
                eq(Instant.parse("2026-06-17T00:00:00Z")))).thenReturn(2L);
        MetricCountProjection status = mock(MetricCountProjection.class);
        when(status.getCode()).thenReturn("ACTIVE");
        when(status.getValue()).thenReturn(6L);
        when(businessRepository.countByStatusCode()).thenReturn(List.of(status));
        BusinessCategoryStatisticsProjection category = mock(BusinessCategoryStatisticsProjection.class);
        when(category.getCategoryId()).thenReturn(3L);
        when(category.getCategoryName()).thenReturn("Nhà hàng");
        when(category.getCategorySlug()).thenReturn("nha-hang");
        when(category.getTotalBusinesses()).thenReturn(5L);
        when(category.getActiveBusinesses()).thenReturn(4L);
        when(category.getPendingBusinesses()).thenReturn(1L);
        when(category.getInactiveOrSuspendedBusinesses()).thenReturn(0L);
        when(businessRepository.countByCategory()).thenReturn(List.of(category));
        BusinessAccountStatisticsProjection account = mock(BusinessAccountStatisticsProjection.class);
        when(account.getBusinessId()).thenReturn(8L);
        when(account.getBusinessName()).thenReturn("Bếp Làng Đường Lâm");
        when(account.getPlaceSlug()).thenReturn("bep-lang-duong-lam");
        when(account.getCategoryName()).thenReturn("Nhà hàng");
        when(account.getCategorySlug()).thenReturn("nha-hang");
        when(account.getStatus()).thenReturn("ACTIVE");
        when(account.getCreatedAt()).thenReturn(Instant.parse("2026-07-01T08:00:00Z"));
        when(account.getApprovedAt()).thenReturn(Instant.parse("2026-07-02T08:00:00Z"));
        when(businessRepository.businessAccountsForStatistics()).thenReturn(List.of(account));

        BusinessStatisticsResponse response = service.businessStatistics(null, null);

        assertEquals(LocalDate.parse("2026-06-17"), response.startDate());
        assertEquals(LocalDate.parse("2026-07-16"), response.endDate());
        assertEquals(11L, response.totalBusinesses());
        assertEquals(6L, response.totalActiveBusinesses());
        assertEquals(2L, response.pendingApprovals());
        assertEquals(2L, response.inactiveOrSuspendedAccounts());
        assertEquals(100.0, response.growthPercent());
        assertEquals("Nhà hàng", response.categoryDistribution().getFirst().categoryName());
        assertEquals("Bếp Làng Đường Lâm", response.accounts().getFirst().businessName());
    }

    @Test
    void monthlyEventStatisticsDefaultsToCurrentMonthAndRanksAttendance() {
        when(authorizationRepository.findEffectiveRoleCodes(10L)).thenReturn(List.of("ADMINISTRATOR"));
        MonthlyEventStatisticsProjection held = mock(MonthlyEventStatisticsProjection.class);
        when(held.getEventId()).thenReturn(3L);
        when(held.getTitle()).thenReturn("Đêm văn hóa Thành cổ");
        when(held.getSlug()).thenReturn("dem-van-hoa-thanh-co");
        when(held.getPlaceName()).thenReturn("Thành cổ Sơn Tây");
        when(held.getLocationNote()).thenReturn("Sân chính");
        when(held.getStartAt()).thenReturn(Instant.parse("2026-07-05T19:00:00Z"));
        when(held.getEndAt()).thenReturn(Instant.parse("2026-07-05T21:00:00Z"));
        when(held.getPeriodStatus()).thenReturn("HISTORICAL");
        when(held.getParticipantRegistrations()).thenReturn(42L);
        when(held.getAuthenticatedParticipants()).thenReturn(12L);
        when(held.getEngagementCount()).thenReturn(50L);
        MonthlyEventStatisticsProjection upcoming = mock(MonthlyEventStatisticsProjection.class);
        when(upcoming.getEventId()).thenReturn(4L);
        when(upcoming.getTitle()).thenReturn("Lễ hội xứ Đoài");
        when(upcoming.getSlug()).thenReturn("le-hoi-xu-doai");
        when(upcoming.getStartAt()).thenReturn(Instant.parse("2026-07-25T08:00:00Z"));
        when(upcoming.getEndAt()).thenReturn(Instant.parse("2026-07-25T11:00:00Z"));
        when(upcoming.getPeriodStatus()).thenReturn("UPCOMING");
        when(upcoming.getParticipantRegistrations()).thenReturn(18L);
        when(upcoming.getAuthenticatedParticipants()).thenReturn(6L);
        when(upcoming.getEngagementCount()).thenReturn(20L);
        when(contentEventRepository.monthlyEventStatistics(
                eq(Instant.parse("2026-07-01T00:00:00Z")),
                eq(Instant.parse("2026-08-01T00:00:00Z")),
                eq(Instant.parse("2026-07-16T09:00:00Z")))).thenReturn(List.of(held, upcoming));
        when(eventRepository.countEventRegistrationsDaily(any(), any())).thenReturn(List.of());

        MonthlyEventStatisticsResponse response = service.monthlyEventStatistics(null, null);

        assertEquals(2026, response.year());
        assertEquals(7, response.month());
        assertEquals(2L, response.totalEvents());
        assertEquals(1L, response.historicalEvents());
        assertEquals(1L, response.upcomingEvents());
        assertEquals(60L, response.participantRegistrations());
        assertEquals("Đêm văn hóa Thành cổ", response.highestAttendedEvent());
    }
}
