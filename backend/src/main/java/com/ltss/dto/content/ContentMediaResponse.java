package com.ltss.dto.content;

import com.ltss.repository.content.TargetMediaProjection;

public record ContentMediaResponse(
        Long id,
        String mediaType,
        String mediaUrl,
        String thumbnailUrl,
        String mimeType,
        String usageType,
        Integer displayOrder,
        boolean primary
) {
    public static ContentMediaResponse from(TargetMediaProjection media) {
        return new ContentMediaResponse(
                media.getMediaId(), media.getMediaType(), media.getMediaUrl(), media.getThumbnailUrl(),
                media.getMimeType(), media.getUsageType(), media.getDisplayOrder(),
                Boolean.TRUE.equals(media.getPrimaryMedia())
        );
    }
}
