package com.ltss.service.content;

import com.ltss.common.response.PageResponse;
import com.ltss.dto.content.*;
import com.ltss.entity.content.*;
import com.ltss.repository.content.*;
import java.util.*;

public interface BusinessPublicService {

    PageResponse<BusinessResponse> businesses(String query, int page, int size);

    BusinessResponse business(Long id);

    PageResponse<BusinessPostSummaryResponse> posts(
            String query,
            Long businessId,
            int page,
            int size
    );

    BusinessPostDetailResponse post(String slug);

    PageResponse<PromotionResponse> promotions(Long businessId, int page, int size);

    PromotionResponse promotion(Long id);
}
