package com.ltss.service.content;

import com.ltss.common.response.PageResponse;
import com.ltss.dto.content.ArticleCategoryResponse;
import com.ltss.dto.content.ArticleDetailResponse;
import com.ltss.dto.content.ArticleSummaryResponse;
import com.ltss.dto.content.EventDetailResponse;
import com.ltss.dto.content.EventSummaryResponse;
import java.util.List;

public interface EditorialPublicService {

    List<ArticleCategoryResponse> categories();

    PageResponse<ArticleSummaryResponse> articles(String query, String categorySlug, int page, int size);

    ArticleDetailResponse article(String slug);

    PageResponse<EventSummaryResponse> events(String query, int page, int size);

    EventDetailResponse event(String slug);
}
