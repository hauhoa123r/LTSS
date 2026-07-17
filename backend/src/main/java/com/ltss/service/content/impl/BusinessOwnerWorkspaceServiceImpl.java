package com.ltss.service.content.impl;

import com.ltss.service.content.BusinessOwnerWorkspaceService;

import com.ltss.common.exception.ResourceNotFoundException;
import com.ltss.common.response.PageResponse;
import com.ltss.repository.auth.AuthorizationRepository;
import com.ltss.security.auth.CurrentUserService;
import com.ltss.dto.content.BusinessOwnerPostResponse;
import com.ltss.dto.content.BusinessOwnerProfileResponse;
import com.ltss.dto.content.BusinessOwnerPromotionResponse;
import com.ltss.dto.content.LinkedPlaceResponse;
import com.ltss.entity.content.*;
import com.ltss.repository.content.BusinessPostRepository;
import com.ltss.repository.content.BusinessRepository;
import com.ltss.repository.content.PromotionRepository;
import com.ltss.entity.place.PlaceEntity;
import com.ltss.repository.place.PlaceRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BusinessOwnerWorkspaceServiceImpl implements BusinessOwnerWorkspaceService {
    private final BusinessRepository businessRepository;
    private final BusinessPostRepository postRepository;
    private final PromotionRepository promotionRepository;
    private final PlaceRepository placeRepository;
    private final AuthorizationRepository authorizationRepository;
    private final CurrentUserService currentUserService;

    public BusinessOwnerWorkspaceServiceImpl(
            BusinessRepository businessRepository,
            BusinessPostRepository postRepository,
            PromotionRepository promotionRepository,
            PlaceRepository placeRepository,
            AuthorizationRepository authorizationRepository,
            CurrentUserService currentUserService
    ) {
        this.businessRepository = businessRepository;
        this.postRepository = postRepository;
        this.promotionRepository = promotionRepository;
        this.placeRepository = placeRepository;
        this.authorizationRepository = authorizationRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    @Override
    public BusinessOwnerProfileResponse profile() {
        BusinessEntity business = ownedBusiness();
        PlaceEntity place = placeRepository.findById(business.getPlaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Business place was not found"));
        return new BusinessOwnerProfileResponse(
                business.getId(), business.getRegistrationNumber(), business.getContactEmail(),
                business.getWebsiteUrl(), business.getStatus(), LinkedPlaceResponse.from(place),
                place.getStatus().name(), business.getVersion()
        );
    }

    @Transactional(readOnly = true)
    @Override
    public PageResponse<BusinessOwnerPostResponse> posts(int page, int size) {
        BusinessEntity business = ownedBusiness();
        return PageResponse.from(postRepository.findAllByBusinessIdAndStatusNotOrderByUpdatedAtDesc(
                business.getId(), PublicationStatus.DELETED, PageRequest.of(page, size)
        ).map(this::post));
    }

    @Transactional(readOnly = true)
    @Override
    public PageResponse<BusinessOwnerPromotionResponse> promotions(int page, int size) {
        BusinessEntity business = ownedBusiness();
        return PageResponse.from(promotionRepository.findAllByBusinessIdAndStatusNotOrderByUpdatedAtDesc(
                business.getId(), PromotionStatus.DELETED, PageRequest.of(page, size)
        ).map(this::promotion));
    }

    private BusinessEntity ownedBusiness() {
        Long actorId = currentUserService.requireUserId();
        if (!authorizationRepository.findEffectiveRoleCodes(actorId).contains("BUSINESS_OWNER")) {
            throw new AccessDeniedException("Business Owner role is required");
        }
        return businessRepository.findByOwnerUserId(actorId)
                .orElseThrow(() -> new ResourceNotFoundException("Owned business was not found"));
    }

    private BusinessOwnerPostResponse post(BusinessPostEntity post) {
        return new BusinessOwnerPostResponse(
                post.getId(), post.getTitle(), post.getSlug(), post.getSummary(), post.getContent(),
                post.getStatus(), post.getVersion(), post.getPublishedAt(), post.getUpdatedAt()
        );
    }

    private BusinessOwnerPromotionResponse promotion(PromotionEntity promotion) {
        return new BusinessOwnerPromotionResponse(
                promotion.getId(), promotion.getTitle(), promotion.getDescription(), promotion.getDiscountType(),
                promotion.getDiscountValue(), promotion.getPromoCode(), promotion.getStartAt(), promotion.getEndAt(),
                promotion.getStatus(), promotion.getVersion(), promotion.getPublishedAt(), promotion.getUpdatedAt()
        );
    }
}
