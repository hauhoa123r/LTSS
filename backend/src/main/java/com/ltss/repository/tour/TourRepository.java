package com.ltss.repository.tour;

import com.ltss.entity.tour.TourEntity;
import com.ltss.entity.tour.TourStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TourRepository extends JpaRepository<TourEntity, Long> {
    Page<TourEntity> findAllByOwnerUserIdAndStatusNotOrderByUpdatedAtDesc(
            Long ownerUserId, TourStatus excludedStatus, Pageable pageable
    );

    @Query("""
            select tour from TourEntity tour
            where tour.status = com.ltss.entity.tour.TourStatus.PUBLISHED
              and tour.visibility = com.ltss.entity.tour.TourVisibility.PUBLIC
              and (:query is null or lower(tour.title) like lower(concat('%', :query, '%')))
            order by tour.publishedAt desc
            """)
    Page<TourEntity> searchPublic(@Param("query") String query, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select tour from TourEntity tour where tour.id = :id")
    Optional<TourEntity> findLockedById(@Param("id") Long id);
}
