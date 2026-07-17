package com.ltss.service.moderation;

import com.ltss.common.response.PageResponse;
import com.ltss.dto.moderation.CancelModerationRequest;
import com.ltss.dto.moderation.ModerationDecisionRequest;
import com.ltss.dto.moderation.ModerationRecordResponse;
import com.ltss.dto.moderation.SubmitModerationRequest;
import com.ltss.entity.moderation.ModerationTargetType;
import com.ltss.service.auth.ClientRequestInfo;

public interface ModerationService {

    ModerationRecordResponse submit(
            ModerationTargetType type,
            Long targetId,
            SubmitModerationRequest request,
            ClientRequestInfo requestInfo
    );

    void registerPendingReview(Long reviewId, Long submitterId, ClientRequestInfo requestInfo);

    PageResponse<ModerationRecordResponse> queue(ModerationTargetType type, int page, int size);

    ModerationRecordResponse detail(Long caseId);

    PageResponse<ModerationRecordResponse> history(
            ModerationTargetType type,
            Long targetId,
            int page,
            int size
    );

    ModerationRecordResponse approve(
            Long caseId,
            ModerationDecisionRequest request,
            ClientRequestInfo requestInfo
    );

    ModerationRecordResponse reject(
            Long caseId,
            ModerationDecisionRequest request,
            ClientRequestInfo requestInfo
    );

    ModerationRecordResponse cancel(
            Long caseId,
            CancelModerationRequest request,
            ClientRequestInfo requestInfo
    );
}
