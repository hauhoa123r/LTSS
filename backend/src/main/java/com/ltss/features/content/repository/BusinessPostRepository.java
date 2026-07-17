package com.ltss.features.content.repository;

import com.ltss.features.content.entity.BusinessPostEntity;
import com.ltss.features.content.entity.PublicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface BusinessPostRepository extends JpaRepository<BusinessPostEntity, Long> {
    @Query("""
            select post from BusinessPostEntity post
            join BusinessEntity business on business.id = post.businessId
            join PlaceEntity place on place.id = business.placeId
            where post.status = com.ltss.features.content.entity.PublicationStatus.PUBLISHED
              and business.status = com.ltss.features.content.entity.BusinessStatus.ACTIVE
              and place.status = com.ltss.features.place.entity.PlaceStatus.PUBLISHED
              and (:businessId is null or post.businessId = :businessId)
              and (:query is null
                   or lower(post.title) like lower(concat('%', :query, '%'))
                   or lower(coalesce(post.summary, '')) like lower(concat('%', :query, '%')))
            order by post.publishedAt desc
            """)
    Page<BusinessPostEntity> searchPublished(
            @Param("query") String query,
            @Param("businessId") Long businessId,
            Pageable pageable
    );

    Optional<BusinessPostEntity> findBySlugAndStatus(String slug, PublicationStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select post from BusinessPostEntity post where post.id = :id")
    Optional<BusinessPostEntity> findLockedById(@Param("id") Long id);
}
