package com.ltss.service.content.impl;

import com.ltss.service.content.RelicArticleService;

import com.ltss.common.exception.ConflictException;
import com.ltss.common.exception.ResourceNotFoundException;
import com.ltss.common.response.PageResponse;
import com.ltss.repository.auth.AuthorizationRepository;
import com.ltss.security.auth.CurrentUserService;
import com.ltss.service.auth.AuditService;
import com.ltss.service.auth.ClientRequestInfo;
import com.ltss.dto.content.RelicArticleResponse;
import com.ltss.dto.content.RelicArticleUpsertRequest;
import com.ltss.entity.content.ArticleCategoryEntity;
import com.ltss.entity.content.ArticleEntity;
import com.ltss.entity.content.PublicationStatus;
import com.ltss.repository.content.ArticleCategoryRepository;
import com.ltss.repository.content.ArticleRepository;
import com.ltss.entity.place.PlaceEntity;
import com.ltss.entity.place.PlaceStatus;
import com.ltss.repository.place.PlaceRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Clock;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
public class RelicArticleServiceImpl implements RelicArticleService {
    private static final String MANAGER_ROLE = "RELIC_MANAGER";

    private final ArticleRepository articleRepository;
    private final ArticleCategoryRepository categoryRepository;
    private final PlaceRepository placeRepository;
    private final AuthorizationRepository authorizationRepository;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;
    private final Clock clock;

    public RelicArticleServiceImpl(
            ArticleRepository articleRepository,
            ArticleCategoryRepository categoryRepository,
            PlaceRepository placeRepository,
            AuthorizationRepository authorizationRepository,
            CurrentUserService currentUserService,
            AuditService auditService,
            Clock clock
    ) {
        this.articleRepository = articleRepository;
        this.categoryRepository = categoryRepository;
        this.placeRepository = placeRepository;
        this.authorizationRepository = authorizationRepository;
        this.currentUserService = currentUserService;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    @Override
    public PageResponse<RelicArticleResponse> mine(int page, int size) {
        Long actorId = requireManager();
        return PageResponse.from(articleRepository
                .findAllByAuthorUserIdAndStatusNotOrderByUpdatedAtDesc(
                        actorId, PublicationStatus.DELETED, PageRequest.of(page, size)
                ).map(this::response));
    }

    @Transactional(readOnly = true)
    @Override
    public RelicArticleResponse detail(Long articleId) {
        Long actorId = requireManager();
        return response(owned(articleId, actorId));
    }

    @Transactional
    @Override
    public RelicArticleResponse create(RelicArticleUpsertRequest request, ClientRequestInfo requestInfo) {
        Long actorId = requireManager();
        ArticleCategoryEntity category = requireCategory(request.categoryId());
        PlaceEntity place = requirePublishedPlace(request.placeId());
        ArticleEntity article = articleRepository.save(new ArticleEntity(
                category.getId(), place.getId(), actorId, normalizeRequired(request.title()),
                uniqueSlug(request.title()), normalize(request.summary()), normalizeRequired(request.content())
        ));
        auditService.recordDomain(actorId, "ARTICLE_CREATED", "ARTICLE", article.getId(), requestInfo);
        return response(article, category, place);
    }

    @Transactional
    @Override
    public RelicArticleResponse update(
            Long articleId, RelicArticleUpsertRequest request, ClientRequestInfo requestInfo
    ) {
        Long actorId = requireManager();
        ArticleEntity article = ownedLocked(articleId, actorId);
        requireEditable(article);
        requireVersion(article, request.version());
        ArticleCategoryEntity category = requireCategory(request.categoryId());
        PlaceEntity place = requirePublishedPlace(request.placeId());
        article.update(
                actorId, category.getId(), place.getId(), normalizeRequired(request.title()),
                normalize(request.summary()), normalizeRequired(request.content())
        );
        auditService.recordDomain(actorId, "ARTICLE_UPDATED", "ARTICLE", articleId, requestInfo);
        return response(article, category, place);
    }

    @Transactional
    @Override
    public void delete(Long articleId, Integer version, ClientRequestInfo requestInfo) {
        Long actorId = requireManager();
        ArticleEntity article = ownedLocked(articleId, actorId);
        requireEditable(article);
        requireVersion(article, version);
        article.delete(clock.instant());
        auditService.recordDomain(actorId, "ARTICLE_DELETED", "ARTICLE", articleId, requestInfo);
    }

    private Long requireManager() {
        Long actorId = currentUserService.requireUserId();
        if (!authorizationRepository.findEffectiveRoleCodes(actorId).contains(MANAGER_ROLE)) {
            throw new AccessDeniedException("Relic Manager role is required");
        }
        return actorId;
    }

    private ArticleEntity owned(Long articleId, Long actorId) {
        ArticleEntity article = articleRepository.findById(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("Article was not found"));
        requireOwner(article, actorId);
        return article;
    }

    private ArticleEntity ownedLocked(Long articleId, Long actorId) {
        ArticleEntity article = articleRepository.findLockedById(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("Article was not found"));
        requireOwner(article, actorId);
        return article;
    }

    private void requireOwner(ArticleEntity article, Long actorId) {
        if (!Objects.equals(article.getAuthorUserId(), actorId)) {
            throw new AccessDeniedException("Article is not owned by this user");
        }
    }

    private ArticleCategoryEntity requireCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .filter(ArticleCategoryEntity::isActive)
                .orElseThrow(() -> new ConflictException("Article category must be active"));
    }

    private PlaceEntity requirePublishedPlace(Long placeId) {
        return placeRepository.findById(placeId)
                .filter(place -> place.getStatus() == PlaceStatus.PUBLISHED)
                .orElseThrow(() -> new ConflictException("Promotional article must belong to a published place"));
    }

    private void requireEditable(ArticleEntity article) {
        if (article.getStatus() != PublicationStatus.DRAFT && article.getStatus() != PublicationStatus.REJECTED) {
            throw new ConflictException("Article cannot be edited from its current state");
        }
    }

    private void requireVersion(ArticleEntity article, Integer version) {
        if (version == null || !Objects.equals(article.getVersion(), version)) {
            throw new ConflictException("Article was changed by another request; reload and try again");
        }
    }

    private RelicArticleResponse response(ArticleEntity article) {
        ArticleCategoryEntity category = categoryRepository.findById(article.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Article category was not found"));
        PlaceEntity place = placeRepository.findById(article.getPlaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Article place was not found"));
        return response(article, category, place);
    }

    private RelicArticleResponse response(
            ArticleEntity article, ArticleCategoryEntity category, PlaceEntity place
    ) {
        return new RelicArticleResponse(
                article.getId(), category.getId(), category.getCategoryName(),
                place.getId(), place.getName(), article.getTitle(), article.getSlug(),
                article.getSummary(), article.getContent(), article.getStatus(), article.getVersion(),
                article.getSubmittedAt(), article.getPublishedAt(), article.getUpdatedAt()
        );
    }

    private String uniqueSlug(String title) {
        String normalized = Normalizer.normalize(title.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd')
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        String base = normalized.isBlank() ? "bai-viet" : normalized;
        if (base.length() > 245) base = base.substring(0, 245).replaceAll("-+$", "");
        String slug;
        do {
            slug = base + "-" + UUID.randomUUID().toString().substring(0, 8);
        } while (articleRepository.existsBySlug(slug));
        return slug;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeRequired(String value) {
        return value.trim();
    }
}
