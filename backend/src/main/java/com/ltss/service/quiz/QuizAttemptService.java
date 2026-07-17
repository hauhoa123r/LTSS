package com.ltss.service.quiz;

import com.ltss.common.response.PageResponse;
import com.ltss.service.auth.ClientRequestInfo;
import com.ltss.dto.quiz.*;
import com.ltss.entity.quiz.*;
import com.ltss.repository.quiz.*;
import java.util.*;

public interface QuizAttemptService {

    QuizAttemptResponse start(Long quizId, StartAttemptRequest request, ClientRequestInfo requestInfo);

    QuizAttemptResponse detail(Long attemptId, ClientRequestInfo requestInfo);

    QuizAttemptResponse submit(Long attemptId, SubmitAttemptRequest request,
                                      ClientRequestInfo requestInfo);

    PageResponse<QuizAttemptSummaryResponse> history(int page, int size);

    PageResponse<AwardedBadgeResponse> badges(int page, int size);
}
