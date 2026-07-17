package com.ltss.features.place.repository;

import com.ltss.features.place.entity.SearchHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.time.Instant;

public interface SearchHistoryRepository extends JpaRepository<SearchHistoryEntity, Long> {
    @Modifying
    @Query(value = """
            INSERT INTO search_history (user_id, keyword, normalized_keyword, searched_at)
            VALUES (:userId, :keyword, :normalizedKeyword, :searchedAt)
            ON DUPLICATE KEY UPDATE
                keyword = VALUES(keyword),
                searched_at = VALUES(searched_at)
            """, nativeQuery = true)
    int upsert(
            @Param("userId") Long userId,
            @Param("keyword") String keyword,
            @Param("normalizedKeyword") String normalizedKeyword,
            @Param("searchedAt") Instant searchedAt
    );

    List<SearchHistoryEntity> findAllByUserIdOrderBySearchedAtDesc(Long userId);

    @Modifying
    @Query("delete from SearchHistoryEntity history where history.userId = :userId")
    int deleteAllForUser(@Param("userId") Long userId);

    @Modifying
    @Query("delete from SearchHistoryEntity history where history.id = :id and history.userId = :userId")
    int deleteOwnedById(@Param("id") Long id, @Param("userId") Long userId);
}
