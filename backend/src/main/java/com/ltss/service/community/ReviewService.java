package com.ltss.service.community;

import com.ltss.common.response.PageResponse;
import com.ltss.service.auth.ClientRequestInfo;
import com.ltss.dto.community.*;
import com.ltss.entity.community.*;
import com.ltss.repository.community.*;
import com.ltss.entity.content.*;
import com.ltss.repository.content.*;
import com.ltss.entity.tour.*;
import org.springframework.data.domain.*;
import java.util.*;

public interface ReviewService {

    ReviewResponse create(ReviewTargetType type, Long targetId, CreateReviewRequest request,
                                 ClientRequestInfo requestInfo);

    PageResponse<ReviewResponse> visible(ReviewTargetType type, Long targetId, int page, int size);

    PageResponse<ReviewResponse> mine(int page, int size);

    ReviewResponse reply(Long reviewId, ReviewReplyRequest request, ClientRequestInfo requestInfo);
}
