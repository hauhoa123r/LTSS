package com.ltss.service.place;

import com.ltss.common.response.PageResponse;
import com.ltss.dto.place.PlaceCategoryResponse;
import com.ltss.dto.place.PlaceDetailResponse;
import com.ltss.dto.place.PlaceSummaryResponse;
import com.ltss.entity.place.PlaceEntity;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface DiscoveryService {

    List<PlaceCategoryResponse> categories();

    PageResponse<PlaceSummaryResponse> search(
            String rawQuery,
            String rawCategorySlug,
            int page,
            int size
    );

    PageResponse<PlaceSummaryResponse> nearby(
            double latitude,
            double longitude,
            double radiusKm,
            String rawCategorySlug,
            int page,
            int size
    );

    PlaceDetailResponse detail(String slug);

    Enrichment enrichment(Collection<Long> placeIds, Long userId);

    PlaceSummaryResponse summary(PlaceEntity place, Double distance, Enrichment enrichment);

    record Enrichment(
            Map<Long, PlaceCategoryResponse> categories,
            Map<Long, String> covers,
            Set<Long> favorites
    ) {
    }
}
