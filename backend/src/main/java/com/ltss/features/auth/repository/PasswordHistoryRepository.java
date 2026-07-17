package com.ltss.features.auth.repository;

import com.ltss.features.auth.entity.PasswordHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PasswordHistoryRepository extends JpaRepository<PasswordHistoryEntity, Long> {
    List<PasswordHistoryEntity> findTop4ByUserIdOrderByCreatedAtDesc(Long userId);
}
