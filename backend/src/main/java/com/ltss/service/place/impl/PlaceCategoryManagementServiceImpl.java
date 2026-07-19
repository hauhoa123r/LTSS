package com.ltss.service.place.impl;

import com.ltss.service.place.PlaceCategoryManagementService;
import com.ltss.common.exception.ConflictException;
import com.ltss.common.exception.ResourceNotFoundException;
import com.ltss.common.response.PageResponse;
import com.ltss.repository.auth.AuthorizationRepository;
import com.ltss.security.auth.CurrentUserService;
import com.ltss.service.auth.AuditService;
import com.ltss.service.auth.ClientRequestInfo;
import com.ltss.dto.place.PlaceCategoryManagementRequest;
import com.ltss.dto.place.PlaceCategoryManagementResponse;
import com.ltss.entity.place.PlaceCategoryEntity;
import com.ltss.repository.place.PlaceCategoryRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;

@Service
public class PlaceCategoryManagementServiceImpl implements PlaceCategoryManagementService {
    private static final Set<String> ALLOWED_ROLES = Set.of("MODERATOR", "ADMINISTRATOR");

    private final PlaceCategoryRepository repository;
    private final AuthorizationRepository authorizationRepository;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;

    public PlaceCategoryManagementServiceImpl(
            PlaceCategoryRepository repository,
            AuthorizationRepository authorizationRepository,
            CurrentUserService currentUserService,
            AuditService auditService
    ) {
        this.repository = repository;
        this.authorizationRepository = authorizationRepository;
        this.currentUserService = currentUserService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    @Override
    public PageResponse<PlaceCategoryManagementResponse> list(int page, int size) {
        requireReviewer();
        return PageResponse.from(repository.findAllByOrderByUpdatedAtDesc(PageRequest.of(page, size)).map(this::response));
    }

    @Transactional(readOnly = true)
    @Override
    public PlaceCategoryManagementResponse get(Long categoryId) {
        requireReviewer();
        return response(requireCategory(categoryId));
    }

    @Transactional
    @Override
    public PlaceCategoryManagementResponse create(
            PlaceCategoryManagementRequest request, ClientRequestInfo requestInfo
    ) {
        Long actorId = requireReviewer();
        String name = normalizeRequired(request.name());
        requireUniqueName(name, null);
        PlaceCategoryEntity category = repository.save(new PlaceCategoryEntity(
                name, uniqueSlug(name, null), normalize(request.description()), request.active(), actorId
        ));
        auditService.recordDomain(actorId, "PLACE_CATEGORY_CREATED", "PLACE_CATEGORY", category.getId(), requestInfo);
        return response(category);
    }

    @Transactional
    @Override
    public PlaceCategoryManagementResponse update(
            Long categoryId, PlaceCategoryManagementRequest request, ClientRequestInfo requestInfo
    ) {
        Long actorId = requireReviewer();
        PlaceCategoryEntity category = requireCategory(categoryId);
        String name = normalizeRequired(request.name());
        requireUniqueName(name, categoryId);
        category.update(
                name, uniqueSlug(name, categoryId), normalize(request.description()), request.active(), actorId
        );
        auditService.recordDomain(actorId, "PLACE_CATEGORY_UPDATED", "PLACE_CATEGORY", categoryId, requestInfo);
        return response(category);
    }

    @Transactional
    @Override
    public void delete(Long categoryId, ClientRequestInfo requestInfo) {
        Long actorId = requireReviewer();
        PlaceCategoryEntity category = requireCategory(categoryId);
        category.deactivate(actorId);
        auditService.recordDomain(actorId, "PLACE_CATEGORY_DEACTIVATED", "PLACE_CATEGORY", categoryId, requestInfo);
    }

    private Long requireReviewer() {
        Long actorId = currentUserService.requireUserId();
        boolean allowed = authorizationRepository.findEffectiveRoleCodes(actorId).stream().anyMatch(ALLOWED_ROLES::contains);
        if (!allowed) throw new AccessDeniedException("Moderator role is required");
        return actorId;
    }

    private PlaceCategoryEntity requireCategory(Long categoryId) {
        return repository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Place category was not found"));
    }

    private void requireUniqueName(String name, Long currentId) {
        boolean exists = currentId == null
                ? repository.existsByCategoryNameIgnoreCase(name)
                : repository.existsByCategoryNameIgnoreCaseAndIdNot(name, currentId);
        if (exists) throw new ConflictException("Place category name already exists");
    }

    private String uniqueSlug(String name, Long currentId) {
        String base = Normalizer.normalize(name.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd')
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (base.isBlank()) base = "danh-muc";
        if (base.length() > 110) base = base.substring(0, 110).replaceAll("-+$", "");
        String slug = base;
        int suffix = 2;
        while (currentId == null ? repository.existsBySlug(slug) : repository.existsBySlugAndIdNot(slug, currentId)) {
            slug = base + "-" + suffix++;
        }
        return slug;
    }

    private PlaceCategoryManagementResponse response(PlaceCategoryEntity category) {
        return new PlaceCategoryManagementResponse(
                category.getId(), category.getCategoryName(), category.getSlug(), category.getDescription(),
                category.isActive(), category.getCreatedAt(), category.getUpdatedAt()
        );
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeRequired(String value) {
        return value.trim();
    }
}
