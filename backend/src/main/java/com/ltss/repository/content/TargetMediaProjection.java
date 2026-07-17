package com.ltss.repository.content;

public interface TargetMediaProjection {
    Long getTargetId();
    Long getMediaId();
    String getMediaType();
    String getMediaUrl();
    String getThumbnailUrl();
    String getMimeType();
    String getUsageType();
    Integer getDisplayOrder();
    Boolean getPrimaryMedia();
}
