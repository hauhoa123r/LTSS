package com.ltss.features.auth.repository;

import com.ltss.features.auth.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<RoleEntity, Long> {
    Optional<RoleEntity> findByRoleCodeAndActiveTrue(String roleCode);
}
