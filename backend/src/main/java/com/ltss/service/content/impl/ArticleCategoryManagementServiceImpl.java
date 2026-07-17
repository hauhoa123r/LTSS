package com.ltss.service.content.impl;

import com.ltss.service.content.ArticleCategoryManagementService;

import com.ltss.common.exception.ConflictException;
import com.ltss.common.exception.ResourceNotFoundException;
import com.ltss.common.response.PageResponse;
import com.ltss.repository.auth.AuthorizationRepository;
import com.ltss.security.auth.CurrentUserService;
import com.ltss.service.auth.AuditService;
import com.ltss.service.auth.ClientRequestInfo;
import com.ltss.dto.content.ArticleCategoryManagementRequest;
import com.ltss.dto.content.ArticleCategoryManagementResponse;
import com.ltss.entity.content.ArticleCategoryEntity;
import com.ltss.repository.content.ArticleCategoryRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;

@Service
public class ArticleCategoryManagementServiceImpl implements ArticleCategoryManagementService {
    private static final Set<String> ALLOWED_ROLES = Set.of("MODERATOR", "ADMINISTRATOR");

    private final ArticleCategoryRepository repository;
    private final AuthorizationRepository authorizationRepository;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;

    public ArticleCategoryManagementServiceImpl(
            ArticleCategoryRepository repository,
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
    public PageResponse<ArticleCategoryManagementResponse> list(int page, int size) {
        requireReviewer();
        return PageResponse.from(repository.findAllByOrderByUpdatedAtDesc(PageRequest.of(page, size)).map(this::response));
    }

    @Transactional
    @Override
    public ArticleCategoryManagementResponse create(
            ArticleCategoryManagementRequest request, ClientRequestInfo requestInfo
    ) {
        Long actorId = requireReviewer();
        String name = normalizeRequired(request.name());
        requireUniqueName(name, null);
        ArticleCategoryEntity category = repository.save(new ArticleCategoryEntity(
                name, uniqueSlug(name, null), normalize(request.description()), request.active(), actorId
        ));
        auditService.recordDomain(actorId, "ARTICLE_CATEGORY_CREATED", "ARTICLE_CATEGORY", category.getId(), requestInfo);
        return response(category);
    }

    @Transactional
    @Override
    public ArticleCategoryManagementResponse update(
            Long categoryId, ArticleCategoryManagementRequest request, ClientRequestInfo requestInfo
    ) {
        Long actorId = requireReviewer();
        ArticleCategoryEntity category = requireCategory(categoryId);
        String name = normalizeRequired(request.name());
        requireUniqueName(name, categoryId);
        category.update(
                name, uniqueSlug(name, categoryId), normalize(request.description()), request.active(), actorId
        );
        auditService.recordDomain(actorId, "ARTICLE_CATEGORY_UPDATED", "ARTICLE_CATEGORY", categoryId, requestInfo);
        return response(category);
    }

    @Transactional
    @Override
    public void delete(Long categoryId, ClientRequestInfo requestInfo) {
        Long actorId = requireReviewer();
        ArticleCategoryEntity category = requireCategory(categoryId);
        category.deactivate(actorId);
        auditService.recordDomain(actorId, "ARTICLE_CATEGORY_DEACTIVATED", "ARTICLE_CATEGORY", categoryId, requestInfo);
    }

    private Long requireReviewer() {
        Long actorId = currentUserService.requireUserId();
        boolean allowed = authorizationRepository.findEffectiveRoleCodes(actorId).stream().anyMatch(ALLOWED_ROLES::contains);
        if (!allowed) throw new AccessDeniedException("Moderator role is required");
        return actorId;
    }

    private ArticleCategoryEntity requireCategory(Long categoryId) {
        return repository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Article category was not found"));
    }

    private void requireUniqueName(String name, Long currentId) {
        boolean exists = currentId == null
                ? repository.existsByCategoryNameIgnoreCase(name)
                : repository.existsByCategoryNameIgnoreCaseAndIdNot(name, currentId);
        if (exists) throw new ConflictException("Article category name already exists");
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

    private ArticleCategoryManagementResponse response(ArticleCategoryEntity category) {
        return new ArticleCategoryManagementResponse(
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
