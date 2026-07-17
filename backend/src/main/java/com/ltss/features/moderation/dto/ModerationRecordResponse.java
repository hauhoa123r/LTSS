package com.ltss.features.moderation.dto;

import com.ltss.features.moderation.entity.ModerationDecision;
import com.ltss.features.moderation.entity.ModerationStatus;
import com.ltss.features.moderation.entity.ModerationTargetType;

import java.time.Instant;

public record ModerationRecordResponse(
        Long id,
        ModerationTargetType targetType,
        Long targetId,
        String targetTitle,
        Integer targetVersion,
        Long submittedByUserId,
        Long moderatorUserId,
        ModerationStatus status,
        ModerationDecision decision,
        String submissionNote,
        String decisionReason,
        Instant submittedAt,
        Instant resolvedAt
) {
}
