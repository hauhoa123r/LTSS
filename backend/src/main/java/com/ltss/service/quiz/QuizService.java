package com.ltss.service.quiz;

import com.ltss.common.response.PageResponse;
import com.ltss.service.auth.ClientRequestInfo;
import com.ltss.dto.quiz.*;
import com.ltss.entity.quiz.*;
import com.ltss.repository.quiz.*;
import java.util.*;

public interface QuizService {

    PageResponse<QuizSummaryResponse> published(Long placeId, int page, int size);

    QuizSummaryResponse publishedDetail(Long quizId);

    PageResponse<QuizSummaryResponse> mine(int page, int size);

    QuizAuthorResponse managementDetail(Long quizId);

    QuizAuthorResponse create(QuizUpsertRequest request, ClientRequestInfo requestInfo);

    QuizAuthorResponse update(Long quizId, QuizUpsertRequest request, ClientRequestInfo requestInfo);

    void delete(Long quizId, Integer version, ClientRequestInfo requestInfo);
}
