package com.ltss.repository.auth;

import com.ltss.entity.auth.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<RoleEntity, Long> {
    Optional<RoleEntity> findByRoleCodeAndActiveTrue(String roleCode);
}
