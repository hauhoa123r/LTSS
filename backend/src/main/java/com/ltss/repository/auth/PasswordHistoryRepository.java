package com.ltss.repository.auth;

import com.ltss.entity.auth.PasswordHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PasswordHistoryRepository extends JpaRepository<PasswordHistoryEntity, Long> {
    List<PasswordHistoryEntity> findTop4ByUserIdOrderByCreatedAtDesc(Long userId);
}
