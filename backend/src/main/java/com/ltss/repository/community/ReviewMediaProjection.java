package com.ltss.repository.community;

public interface ReviewMediaProjection {
    Long getReviewId();
    Long getMediaId();
    String getMediaUrl();
    String getThumbnailUrl();
    Integer getDisplayOrder();
}
