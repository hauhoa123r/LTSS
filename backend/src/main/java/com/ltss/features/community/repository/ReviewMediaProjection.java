package com.ltss.features.community.repository;

public interface ReviewMediaProjection {
    Long getReviewId();
    Long getMediaId();
    String getMediaUrl();
    String getThumbnailUrl();
    Integer getDisplayOrder();
}
