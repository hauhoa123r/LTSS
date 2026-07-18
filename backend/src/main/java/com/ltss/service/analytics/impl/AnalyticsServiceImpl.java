package com.ltss.service.analytics.impl;

import com.ltss.service.analytics.AnalyticsService;

import com.ltss.common.exception.BusinessStatisticsUnavailableException;
import com.ltss.common.exception.BusinessRuleViolationException;
import com.ltss.common.exception.MonumentStatisticsUnavailableException;
import com.ltss.common.exception.MonthlyEventStatisticsUnavailableException;
import com.ltss.dto.analytics.*;
import com.ltss.repository.analytics.*;
import com.ltss.entity.auth.UserStatus;
import com.ltss.repository.auth.*;
import com.ltss.security.auth.CurrentUserService;
import com.ltss.entity.content.*;
import com.ltss.repository.content.BusinessRepository;
import com.ltss.repository.content.EventRepository;
import com.ltss.entity.place.PlaceStatus;
import com.ltss.repository.place.PlaceRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalyticsServiceImpl implements AnalyticsService {
    private final EngagementEventRepository eventRepository;
    private final BusinessRepository businessRepository;
    private final EventRepository contentEventRepository;
    private final PlaceRepository placeRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuthorizationRepository authorizationRepository;
    private final CurrentUserService currentUserService;
    private final Clock clock;

    public AnalyticsServiceImpl(EngagementEventRepository eventRepository, BusinessRepository businessRepository,
                            EventRepository contentEventRepository, PlaceRepository placeRepository, UserRepository userRepository,
                            AuditLogRepository auditLogRepository, AuthorizationRepository authorizationRepository,
                            CurrentUserService currentUserService, Clock clock) {
        this.eventRepository = eventRepository;
        this.businessRepository = businessRepository;
        this.contentEventRepository = contentEventRepository;
        this.placeRepository = placeRepository;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.authorizationRepository = authorizationRepository;
        this.currentUserService = currentUserService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    @Override
    public AnalyticsOverviewResponse system(LocalDate from, LocalDate to) {
        requireAdministrator();
        Range range = range(from, to);
        return overview(range, null, null);
    }

    @Transactional(readOnly = true)
    @Override
    public AnalyticsOverviewResponse ownBusiness(LocalDate from, LocalDate to) {
        Long actorId = currentUserService.requireUserId();
        BusinessEntity business = businessRepository.findByOwnerUserId(actorId)
                .filter(item -> item.getStatus() == BusinessStatus.ACTIVE)
                .orElseThrow(() -> new AccessDeniedException("An active owned business is required"));
        Range range = range(from, to);
        return overview(range, business, placeRepository.findById(business.getPlaceId())
                .map(item -> item.getName()).orElse("Doanh nghiệp #" + business.getId()));
    }

    @Transactional(readOnly = true)
    @Override
    public AdminDashboardResponse dashboard(LocalDate from, LocalDate to) {
        requireAdministrator();
        Map<String, Long> users = new LinkedHashMap<>();
        for (UserStatus status : UserStatus.values()) users.put(status.name(), userRepository.countByStatus(status));
        return new AdminDashboardResponse(
                users, placeRepository.countByStatus(PlaceStatus.PUBLISHED),
                businessRepository.countByStatus(BusinessStatus.ACTIVE), overview(range(from, to), null, null)
        );
    }

    @Transactional(readOnly = true)
    @Override
    public MonumentStatisticsResponse monumentStatistics(LocalDate startDate, LocalDate endDate,
                                                         MonumentGranularity granularity) {
        requireAdministrator();
        Range range = monumentRange(startDate, endDate);
        try {
            long totalVisits = eventRepository.countPlaceViews(range.from, range.toExclusive);
            Range previousRange = previousRange(range);
            long previousTotalVisits = eventRepository.countPlaceViews(previousRange.from, previousRange.toExclusive);
            List<DailyCountResponse> trends = placeViewTrend(range, granularity);
            List<DailyCountResponse> dailyTrends = granularity == MonumentGranularity.DAILY
                    ? trends
                    : placeViewTrend(range, MonumentGranularity.DAILY);
            DailyCountResponse peak = trends.stream()
                    .max(Comparator.comparingLong(DailyCountResponse::value))
                    .orElse(new DailyCountResponse(range.fromDate, 0));
            Map<Long, Long> previousByMonument = eventRepository
                    .countPlaceViewsByMonument(previousRange.from, previousRange.toExclusive).stream()
                    .collect(Collectors.toMap(MonumentCountProjection::getPlaceId, MonumentCountProjection::getVisits));
            Map<Long, List<DailyCountResponse>> trendByMonument = eventRepository
                    .countPlaceViewsDailyByMonument(range.from, range.toExclusive).stream()
                    .collect(Collectors.groupingBy(
                            MonumentTrendProjection::getPlaceId,
                            LinkedHashMap::new,
                            Collectors.mapping(item -> new DailyCountResponse(item.getDay(), item.getValue()), Collectors.toList())
                    ));
            long days = ChronoUnit.DAYS.between(range.fromDate, range.toDate) + 1;
            List<MonumentVisitResponse> monuments = eventRepository.countVisitsByMonument(range.from, range.toExclusive)
                    .stream()
                    .map(item -> new MonumentVisitResponse(
                            item.getPlaceId(), item.getName(), item.getSlug(), item.getAddress(),
                            item.getVisits(), item.getUniqueSessions(), item.getAuthenticatedVisitors(),
                            growthPercent(item.getVisits(), previousByMonument.getOrDefault(item.getPlaceId(), 0L)),
                            averagePerDay(item.getVisits(), days),
                            item.getLastVisitAt(),
                            trendByMonument.getOrDefault(item.getPlaceId(), List.of())
                    )).toList();
            String topMonument = monuments.stream()
                    .max(Comparator.comparingLong(MonumentVisitResponse::visits))
                    .filter(item -> item.visits() > 0)
                    .map(MonumentVisitResponse::name)
                    .orElse(null);
            return new MonumentStatisticsResponse(
                    range.fromDate,
                    range.toDate,
                    granularity,
                    clock.instant(),
                    totalVisits,
                    previousTotalVisits,
                    growthPercent(totalVisits, previousTotalVisits),
                    averagePerDay(totalVisits, days),
                    peak.day(),
                    peak.value(),
                    eventRepository.countPlaceViewSessions(range.from, range.toExclusive),
                    eventRepository.countPlaceViewUsers(range.from, range.toExclusive),
                    monuments.stream().filter(item -> item.visits() > 0).count(),
                    topMonument,
                    trends,
                    dailyTrends,
                    monuments
            );
        } catch (DataAccessException exception) {
            throw new MonumentStatisticsUnavailableException();
        }
    }

    @Transactional(readOnly = true)
    @Override
    public BusinessStatisticsResponse businessStatistics(LocalDate startDate, LocalDate endDate) {
        requireAdministrator();
        Range range = monumentRange(startDate, endDate);
        try {
            Range previousRange = previousRange(range);
            long active = businessRepository.countByStatus(BusinessStatus.ACTIVE);
            long pending = businessRepository.countByStatus(BusinessStatus.PENDING);
            long suspended = businessRepository.countByStatus(BusinessStatus.SUSPENDED);
            long inactive = businessRepository.countByStatus(BusinessStatus.INACTIVE);
            long rejected = businessRepository.countByStatus(BusinessStatus.REJECTED);
            long currentRegistrations = businessRepository.countCreatedInRange(range.from, range.toExclusive);
            long previousRegistrations = businessRepository.countCreatedInRange(previousRange.from, previousRange.toExclusive);
            long total = active + pending + suspended + inactive + rejected;
            List<BusinessCategoryStatisticsResponse> categories = businessRepository.countByCategory().stream()
                    .map(item -> new BusinessCategoryStatisticsResponse(
                            item.getCategoryId(),
                            item.getCategoryName(),
                            item.getCategorySlug(),
                            safeLong(item.getTotalBusinesses()),
                            safeLong(item.getActiveBusinesses()),
                            safeLong(item.getPendingBusinesses()),
                            safeLong(item.getInactiveOrSuspendedBusinesses()),
                            percentage(safeLong(item.getTotalBusinesses()), total)
                    )).toList();
            List<BusinessAccountStatisticsResponse> accounts = businessRepository.businessAccountsForStatistics().stream()
                    .map(item -> new BusinessAccountStatisticsResponse(
                            item.getBusinessId(),
                            item.getBusinessName(),
                            item.getPlaceSlug(),
                            item.getCategoryName(),
                            item.getCategorySlug(),
                            item.getStatus(),
                            item.getCreatedAt(),
                            item.getApprovedAt()
                    )).toList();
            return new BusinessStatisticsResponse(
                    range.fromDate,
                    range.toDate,
                    clock.instant(),
                    total,
                    active,
                    pending,
                    inactive + suspended,
                    rejected,
                    currentRegistrations,
                    previousRegistrations,
                    growthPercent(currentRegistrations, previousRegistrations),
                    businessRepository.countByStatusCode().stream()
                            .map(item -> new MetricCountResponse(item.getCode(), item.getValue())).toList(),
                    categories,
                    accounts
            );
        } catch (DataAccessException exception) {
            throw new BusinessStatisticsUnavailableException();
        }
    }

    @Transactional(readOnly = true)
    @Override
    public MonthlyEventStatisticsResponse monthlyEventStatistics(Integer year, Integer month) {
        requireAdministrator();
        YearMonth selectedMonth = eventMonth(year, month);
        Range range = range(selectedMonth.atDay(1), selectedMonth.atEndOfMonth());
        try {
            List<MonthlyEventSummaryResponse> events = contentEventRepository
                    .monthlyEventStatistics(range.from, range.toExclusive, clock.instant()).stream()
                    .map(item -> new MonthlyEventSummaryResponse(
                            item.getEventId(),
                            item.getTitle(),
                            item.getSlug(),
                            item.getPlaceName(),
                            item.getLocationNote(),
                            item.getStartAt(),
                            item.getEndAt(),
                            item.getPeriodStatus(),
                            safeLong(item.getParticipantRegistrations()),
                            safeLong(item.getAuthenticatedParticipants()),
                            safeLong(item.getEngagementCount())
                    )).toList();
            long historicalEvents = events.stream().filter(item -> "HISTORICAL".equals(item.periodStatus())).count();
            long activeEvents = events.stream().filter(item -> "ACTIVE".equals(item.periodStatus())).count();
            long upcomingEvents = events.stream().filter(item -> "UPCOMING".equals(item.periodStatus())).count();
            long registrations = events.stream().mapToLong(MonthlyEventSummaryResponse::participantRegistrations).sum();
            long authenticated = events.stream().mapToLong(MonthlyEventSummaryResponse::authenticatedParticipants).sum();
            MonthlyEventSummaryResponse highest = events.stream()
                    .max(Comparator.comparingLong(MonthlyEventSummaryResponse::participantRegistrations)
                            .thenComparingLong(MonthlyEventSummaryResponse::engagementCount))
                    .orElse(null);
            return new MonthlyEventStatisticsResponse(
                    selectedMonth.getYear(),
                    selectedMonth.getMonthValue(),
                    range.fromDate,
                    range.toDate,
                    clock.instant(),
                    events.size(),
                    historicalEvents,
                    activeEvents,
                    upcomingEvents,
                    registrations,
                    authenticated,
                    highest == null || highest.participantRegistrations() == 0 ? null : highest.title(),
                    highest == null ? 0 : highest.participantRegistrations(),
                    List.of(
                            new MetricCountResponse("HISTORICAL", historicalEvents),
                            new MetricCountResponse("ACTIVE", activeEvents),
                            new MetricCountResponse("UPCOMING", upcomingEvents)
                    ),
                    eventRepository.countEventRegistrationsDaily(range.from, range.toExclusive).stream()
                            .map(item -> new DailyCountResponse(item.getDay(), item.getValue())).toList(),
                    events
            );
        } catch (DataAccessException exception) {
            throw new MonthlyEventStatisticsUnavailableException();
        }
    }

    @Transactional(readOnly = true)
    @Override
    public RetentionStatusResponse retentionStatus() {
        requireAdministrator();
        return new RetentionStatusResponse(
                false, eventRepository.count(), eventRepository.findOldestOccurredAt(),
                auditLogRepository.count(), auditLogRepository.findOldestCreatedAt(),
                "Deletion is disabled until the analytics and audit retention policy is approved"
        );
    }

    private AnalyticsOverviewResponse overview(Range range, BusinessEntity business, String businessName) {
        if (business == null) {
            return new AnalyticsOverviewResponse(
                    range.fromDate, range.toDate, null, null,
                    eventRepository.countInRange(range.from, range.toExclusive),
                    eventRepository.countUniqueSessions(range.from, range.toExclusive),
                    eventRepository.countAuthenticatedUsers(range.from, range.toExclusive),
                    eventRepository.countByType(range.from, range.toExclusive).stream()
                            .map(item -> new MetricCountResponse(item.getCode(), item.getValue())).toList(),
                    eventRepository.countDaily(range.from, range.toExclusive).stream()
                            .map(item -> new DailyCountResponse(item.getDay(), item.getValue())).toList()
            );
        }
        Long businessId = business.getId();
        Long placeId = business.getPlaceId();
        return new AnalyticsOverviewResponse(
                range.fromDate, range.toDate, businessId, businessName,
                eventRepository.countBusinessEvents(businessId, placeId, range.from, range.toExclusive),
                eventRepository.countBusinessSessions(businessId, placeId, range.from, range.toExclusive),
                eventRepository.countBusinessUsers(businessId, placeId, range.from, range.toExclusive),
                eventRepository.countBusinessByType(businessId, placeId, range.from, range.toExclusive).stream()
                        .map(item -> new MetricCountResponse(item.getCode(), item.getValue())).toList(),
                eventRepository.countBusinessDaily(businessId, placeId, range.from, range.toExclusive).stream()
                        .map(item -> new DailyCountResponse(item.getDay(), item.getValue())).toList()
        );
    }

    private Range range(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)) {
            throw new BusinessRuleViolationException("Analytics date range is invalid");
        }
        return new Range(from, to, from.atStartOfDay(ZoneOffset.UTC).toInstant(),
                to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant());
    }

    private Range monumentRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null) {
            LocalDate today = LocalDate.now(clock);
            return range(today.minusDays(29), today);
        }
        if (startDate == null || endDate == null) {
            throw new BusinessRuleViolationException("Analytics date range is invalid");
        }
        return range(startDate, endDate);
    }

    private YearMonth eventMonth(Integer year, Integer month) {
        if (year == null && month == null) return YearMonth.now(clock);
        if (year == null || month == null || month < 1 || month > 12) {
            throw new BusinessRuleViolationException("Analytics month is invalid");
        }
        return YearMonth.of(year, month);
    }

    private Range previousRange(Range range) {
        long days = ChronoUnit.DAYS.between(range.fromDate, range.toDate) + 1;
        LocalDate previousStart = range.fromDate.minusDays(days);
        LocalDate previousEnd = range.fromDate.minusDays(1);
        return range(previousStart, previousEnd);
    }

    private List<DailyCountResponse> placeViewTrend(Range range, MonumentGranularity granularity) {
        List<DailyCountProjection> projection = switch (granularity) {
            case WEEKLY -> eventRepository.countPlaceViewsWeekly(range.from, range.toExclusive);
            case MONTHLY -> eventRepository.countPlaceViewsMonthly(range.from, range.toExclusive);
            case DAILY -> eventRepository.countPlaceViewsDaily(range.from, range.toExclusive);
        };
        return projection.stream().map(item -> new DailyCountResponse(item.getDay(), item.getValue())).toList();
    }

    private double averagePerDay(long total, long days) {
        if (days <= 0) return 0;
        return Math.round((total / (double) days) * 10.0) / 10.0;
    }

    private double growthPercent(long current, long previous) {
        if (previous == 0) return current > 0 ? 100.0 : 0.0;
        return Math.round(((current - previous) * 1000.0) / previous) / 10.0;
    }

    private double percentage(long value, long total) {
        if (total == 0) return 0.0;
        return Math.round((value * 1000.0) / total) / 10.0;
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private void requireAdministrator() {
        Long actorId = currentUserService.requireUserId();
        if (!authorizationRepository.findEffectiveRoleCodes(actorId).contains("ADMINISTRATOR")) {
            throw new AccessDeniedException("Administrator role is required");
        }
    }

    private record Range(LocalDate fromDate, LocalDate toDate, Instant from, Instant toExclusive) {}
}
