package com.ltss.repository.content;

import com.ltss.entity.content.BusinessEntity;
import com.ltss.entity.content.BusinessStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BusinessRepository extends JpaRepository<BusinessEntity, Long> {
    @Query("""
            select business from BusinessEntity business
            join PlaceEntity place on place.id = business.placeId
            where business.status = com.ltss.entity.content.BusinessStatus.ACTIVE
              and place.status = com.ltss.entity.place.PlaceStatus.PUBLISHED
              and (:query is null or lower(place.name) like lower(concat('%', :query, '%')))
            order by place.name asc
            """)
    Page<BusinessEntity> searchActive(@Param("query") String query, Pageable pageable);

    Optional<BusinessEntity> findByIdAndStatus(Long id, BusinessStatus status);
    Optional<BusinessEntity> findByOwnerUserId(Long ownerUserId);
    long countByStatus(BusinessStatus status);
}
