package com.ltss.entity.place;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Entity
@Table(name = "search_history")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SearchHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 255)
    private String keyword;

    @Column(name = "normalized_keyword", nullable = false, length = 255)
    private String normalizedKeyword;

    @Column(name = "searched_at", nullable = false)
    private Instant searchedAt;

    public SearchHistoryEntity(Long userId, String keyword, String normalizedKeyword, Instant searchedAt) {
        this.userId = userId;
        this.keyword = keyword;
        this.normalizedKeyword = normalizedKeyword;
        this.searchedAt = searchedAt;
    }

    public void searchedAgain(String latestKeyword, Instant now) {
        keyword = latestKeyword;
        searchedAt = now;
    }
}
