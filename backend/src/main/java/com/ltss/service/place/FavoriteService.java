package com.ltss.service.place;

import com.ltss.common.response.PageResponse;
import com.ltss.dto.place.FavoriteStateResponse;
import com.ltss.dto.place.PlaceSummaryResponse;

public interface FavoriteService {

    FavoriteStateResponse add(Long placeId);

    FavoriteStateResponse remove(Long placeId);

    PageResponse<PlaceSummaryResponse> listMine(int page, int size);
}
