package com.ltss.service.content;

import com.ltss.common.response.PageResponse;
import com.ltss.service.auth.ClientRequestInfo;
import com.ltss.dto.content.RelicArticleResponse;
import com.ltss.dto.content.RelicArticleUpsertRequest;

public interface RelicArticleService {

    PageResponse<RelicArticleResponse> mine(int page, int size);

    RelicArticleResponse detail(Long articleId);

    RelicArticleResponse create(RelicArticleUpsertRequest request, ClientRequestInfo requestInfo);

    RelicArticleResponse update(
            Long articleId, RelicArticleUpsertRequest request, ClientRequestInfo requestInfo
    );

    void delete(Long articleId, Integer version, ClientRequestInfo requestInfo);
}
