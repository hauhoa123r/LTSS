package com.ltss.features.place.dto;

import com.ltss.features.place.entity.SearchHistoryEntity;

import java.time.Instant;

public record SearchHistoryResponse(Long id, String keyword, Instant searchedAt) {
    public static SearchHistoryResponse from(SearchHistoryEntity history) {
        return new SearchHistoryResponse(history.getId(), history.getKeyword(), history.getSearchedAt());
    }
}
