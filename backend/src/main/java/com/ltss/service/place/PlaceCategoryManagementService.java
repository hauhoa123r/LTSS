package com.ltss.service.place;

import com.ltss.common.response.PageResponse;
import com.ltss.service.auth.ClientRequestInfo;
import com.ltss.dto.place.PlaceCategoryManagementRequest;
import com.ltss.dto.place.PlaceCategoryManagementResponse;

public interface PlaceCategoryManagementService {
    PageResponse<PlaceCategoryManagementResponse> list(int page, int size);
    PlaceCategoryManagementResponse get(Long categoryId);
    PlaceCategoryManagementResponse create(PlaceCategoryManagementRequest request, ClientRequestInfo requestInfo);
    PlaceCategoryManagementResponse update(Long categoryId, PlaceCategoryManagementRequest request, ClientRequestInfo requestInfo);
    void delete(Long categoryId, ClientRequestInfo requestInfo);
}
