package com.ltss.service.place.impl;

import com.ltss.service.place.SearchHistoryService;

import com.ltss.security.auth.CurrentUserService;
import com.ltss.dto.place.SearchHistoryResponse;
import com.ltss.entity.place.SearchHistoryEntity;
import com.ltss.repository.place.SearchHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Clock;
import java.util.List;
import java.util.Locale;

@Service
public class SearchHistoryServiceImpl implements SearchHistoryService {
    private static final int MAX_HISTORY = 10;

    private final SearchHistoryRepository repository;
    private final CurrentUserService currentUserService;
    private final Clock clock;

    public SearchHistoryServiceImpl(
            SearchHistoryRepository repository,
            CurrentUserService currentUserService,
            Clock clock
    ) {
        this.repository = repository;
        this.currentUserService = currentUserService;
        this.clock = clock;
    }

    @Transactional
    @Override
    public void record(Long userId, String rawKeyword) {
        String keyword = rawKeyword.trim().replaceAll("\\s+", " ");
        String normalized = normalize(keyword);
        repository.upsert(userId, keyword, normalized, clock.instant());

        List<SearchHistoryEntity> history = repository.findAllByUserIdOrderBySearchedAtDesc(userId);
        if (history.size() > MAX_HISTORY) {
            repository.deleteAllInBatch(history.subList(MAX_HISTORY, history.size()));
        }
    }

    @Transactional(readOnly = true)
    @Override
    public List<SearchHistoryResponse> listMine() {
        return repository.findAllByUserIdOrderBySearchedAtDesc(currentUserService.requireUserId()).stream()
                .limit(MAX_HISTORY)
                .map(SearchHistoryResponse::from)
                .toList();
    }

    @Transactional
    @Override
    public void clearMine() {
        repository.deleteAllForUser(currentUserService.requireUserId());
    }

    @Transactional
    @Override
    public boolean deleteMine(Long historyId) {
        return repository.deleteOwnedById(historyId, currentUserService.requireUserId()) > 0;
    }

    String normalize(String keyword) {
        return Normalizer.normalize(keyword, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("\\s+", " ");
    }
}
