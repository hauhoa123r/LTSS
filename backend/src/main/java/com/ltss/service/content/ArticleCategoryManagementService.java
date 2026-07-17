package com.ltss.service.content;

import com.ltss.common.response.PageResponse;
import com.ltss.service.auth.ClientRequestInfo;
import com.ltss.dto.content.ArticleCategoryManagementRequest;
import com.ltss.dto.content.ArticleCategoryManagementResponse;

public interface ArticleCategoryManagementService {

    PageResponse<ArticleCategoryManagementResponse> list(int page, int size);

    ArticleCategoryManagementResponse create(
            ArticleCategoryManagementRequest request, ClientRequestInfo requestInfo
    );

    ArticleCategoryManagementResponse update(
            Long categoryId, ArticleCategoryManagementRequest request, ClientRequestInfo requestInfo
    );

    void delete(Long categoryId, ClientRequestInfo requestInfo);
}
