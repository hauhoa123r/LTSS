package com.ltss.features.analytics.service;

import com.ltss.common.exception.BusinessRuleViolationException;
import com.ltss.features.analytics.dto.*;
import com.ltss.features.analytics.repository.*;
import com.ltss.features.auth.entity.UserStatus;
import com.ltss.features.auth.repository.*;
import com.ltss.features.auth.security.CurrentUserService;
import com.ltss.features.content.entity.*;
import com.ltss.features.content.repository.BusinessRepository;
import com.ltss.features.place.entity.PlaceStatus;
import com.ltss.features.place.repository.PlaceRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
public class AnalyticsService {
    private final EngagementEventRepository eventRepository;
    private final BusinessRepository businessRepository;
    private final PlaceRepository placeRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuthorizationRepository authorizationRepository;
    private final CurrentUserService currentUserService;

    public AnalyticsService(EngagementEventRepository eventRepository, BusinessRepository businessRepository,
                            PlaceRepository placeRepository, UserRepository userRepository,
                            AuditLogRepository auditLogRepository, AuthorizationRepository authorizationRepository,
                            CurrentUserService currentUserService) {
        this.eventRepository = eventRepository;
        this.businessRepository = businessRepository;
        this.placeRepository = placeRepository;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.authorizationRepository = authorizationRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public AnalyticsOverviewResponse system(LocalDate from, LocalDate to) {
        requireAdministrator();
        Range range = range(from, to);
        return overview(range, null, null);
    }

    @Transactional(readOnly = true)
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

    private void requireAdministrator() {
        Long actorId = currentUserService.requireUserId();
        if (!authorizationRepository.findEffectiveRoleCodes(actorId).contains("ADMINISTRATOR")) {
            throw new AccessDeniedException("Administrator role is required");
        }
    }

    private record Range(LocalDate fromDate, LocalDate toDate, Instant from, Instant toExclusive) {}
}
