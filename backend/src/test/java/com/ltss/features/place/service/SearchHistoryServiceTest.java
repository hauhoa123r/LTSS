package com.ltss.features.place.service;

import com.ltss.features.auth.security.CurrentUserService;
import com.ltss.features.place.entity.SearchHistoryEntity;
import com.ltss.features.place.repository.SearchHistoryRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SearchHistoryServiceTest {
    private final SearchHistoryRepository repository = mock(SearchHistoryRepository.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final Instant now = Instant.parse("2026-07-16T06:00:00Z");
    private final SearchHistoryService service = new SearchHistoryService(
            repository,
            currentUserService,
            Clock.fixed(now, ZoneOffset.UTC)
    );

    @Test
    void normalizesVietnameseKeywordForAccentInsensitiveUniqueness() {
        assertThat(service.normalize("Đền Và  Cổ")).isEqualTo("den va co");
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
