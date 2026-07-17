package com.ltss.service.content;

import com.ltss.common.response.PageResponse;
import com.ltss.dto.content.BusinessOwnerPostResponse;
import com.ltss.dto.content.BusinessOwnerProfileResponse;
import com.ltss.dto.content.BusinessOwnerPromotionResponse;
import com.ltss.entity.content.*;

public interface BusinessOwnerWorkspaceService {

    BusinessOwnerProfileResponse profile();

    PageResponse<BusinessOwnerPostResponse> posts(int page, int size);

    PageResponse<BusinessOwnerPromotionResponse> promotions(int page, int size);
}
