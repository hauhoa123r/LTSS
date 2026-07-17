package com.ltss.service.tour;

import com.ltss.common.response.PageResponse;
import com.ltss.service.auth.ClientRequestInfo;
import com.ltss.dto.tour.*;
import com.ltss.entity.tour.*;
import java.util.*;

public interface TourService {

    TourDetailResponse create(TourUpsertRequest request, ClientRequestInfo requestInfo);

    TourDetailResponse update(Long tourId, TourUpsertRequest request, ClientRequestInfo requestInfo);

    PageResponse<TourSummaryResponse> mine(int page, int size);

    PageResponse<TourSummaryResponse> publicTours(String query, int page, int size);

    TourDetailResponse detail(Long tourId);

    TourDetailResponse copy(Long sourceId, ClientRequestInfo requestInfo);

    TourDetailResponse changeVisibility(Long tourId, ChangeTourVisibilityRequest request,
                                               ClientRequestInfo requestInfo);

    void delete(Long tourId, Integer version, ClientRequestInfo requestInfo);
}
