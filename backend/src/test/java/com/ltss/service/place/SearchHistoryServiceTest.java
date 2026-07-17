package com.ltss.service.place;

import com.ltss.service.place.impl.SearchHistoryServiceImpl;

import com.ltss.security.auth.CurrentUserService;
import com.ltss.entity.place.SearchHistoryEntity;
import com.ltss.repository.place.SearchHistoryRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.IntStream;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SearchHistoryServiceTest {
    private final SearchHistoryRepository repository = mock(SearchHistoryRepository.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final Instant now = Instant.parse("2026-07-16T06:00:00Z");
    private final SearchHistoryService service = new SearchHistoryServiceImpl(
            repository,
            currentUserService,
            Clock.fixed(now, ZoneOffset.UTC)
    );

    @Test
    void normalizesVietnameseKeywordForAccentInsensitiveUniqueness() {
        when(repository.findAllByUserIdOrderBySearchedAtDesc(9L)).thenReturn(List.of());

        service.record(9L, "Đền Và  Cổ");

        verify(repository).upsert(9L, "Đền Và Cổ", "den va co", now);
    }

    @Test
    void upsertsSearchAndPrunesHistoryToTenRows() {
        List<SearchHistoryEntity> rows = IntStream.range(0, 11)
                .mapToObj(index -> new SearchHistoryEntity(
                        9L, "Từ khóa " + index, "tu khoa " + index, now.minusSeconds(index)
                ))
                .toList();
        when(repository.findAllByUserIdOrderBySearchedAtDesc(9L)).thenReturn(rows);

        service.record(9L, "  Đền   Và ");

        verify(repository).upsert(9L, "Đền Và", "den va", now);
        verify(repository).deleteAllInBatch(rows.subList(10, 11));
    }
}
