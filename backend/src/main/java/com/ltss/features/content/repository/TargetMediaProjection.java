package com.ltss.features.content.repository;

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
