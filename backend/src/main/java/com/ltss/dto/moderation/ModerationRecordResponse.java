package com.ltss.dto.moderation;

import com.ltss.entity.moderation.ModerationDecision;
import com.ltss.entity.moderation.ModerationStatus;
import com.ltss.entity.moderation.ModerationTargetType;

import java.time.Instant;

public record ModerationRecordResponse(
        Long id,
        ModerationTargetType targetType,
        Long targetId,
        String targetTitle,
        Integer targetVersion,
        Long submittedByUserId,
        String submittedByDisplayName,
        Long moderatorUserId,
        ModerationStatus status,
        boolean actionable,
        ModerationDecision decision,
        String submissionNote,
        String decisionReason,
        Instant submittedAt,
        Instant resolvedAt,
        boolean contentSnapshot,
        ModerationTargetContentResponse targetContent
) {
}
