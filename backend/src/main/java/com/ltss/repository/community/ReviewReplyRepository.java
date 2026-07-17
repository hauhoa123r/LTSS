package com.ltss.repository.community;

import com.ltss.entity.community.ReviewReplyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface ReviewReplyRepository extends JpaRepository<ReviewReplyEntity, Long> {
    Optional<ReviewReplyEntity> findByReviewId(Long reviewId);
    List<ReviewReplyEntity> findAllByReviewIdIn(Collection<Long> reviewIds);
}
