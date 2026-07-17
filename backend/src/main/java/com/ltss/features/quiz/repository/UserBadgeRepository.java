package com.ltss.features.quiz.repository;

import com.ltss.features.quiz.entity.UserBadgeEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserBadgeRepository extends JpaRepository<UserBadgeEntity, Long> {
    boolean existsByUserIdAndBadgeId(Long userId, Long badgeId);
    Page<UserBadgeEntity> findAllByUserIdOrderByAwardedAtDesc(Long userId, Pageable pageable);
    List<UserBadgeEntity> findAllByAwardedAttemptIdOrderByAwardedAtAsc(Long attemptId);
}
