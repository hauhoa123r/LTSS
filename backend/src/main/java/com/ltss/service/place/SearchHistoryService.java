package com.ltss.service.place;

import com.ltss.dto.place.SearchHistoryResponse;
import java.util.List;

public interface SearchHistoryService {

    void record(Long userId, String rawKeyword);

    List<SearchHistoryResponse> listMine();

    void clearMine();

    boolean deleteMine(Long historyId);
}
