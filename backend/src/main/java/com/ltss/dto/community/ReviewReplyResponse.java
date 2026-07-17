package com.ltss.dto.community;

import java.time.Instant;

public record ReviewReplyResponse(
        Long id, Long repliedByUserId, String repliedByName, String content, Integer version, Instant createdAt
) {}
