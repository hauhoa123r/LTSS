package com.ltss.features.community.repository;

import com.ltss.features.community.entity.ReviewReplyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface ReviewReplyRepository extends JpaRepository<ReviewReplyEntity, Long> {
    Optional<ReviewReplyEntity> findByReviewId(Long reviewId);
    List<ReviewReplyEntity> findAllByReviewIdIn(Collection<Long> reviewIds);
}
