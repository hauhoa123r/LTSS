package com.ltss.repository.auth;

import com.ltss.entity.auth.UserRoleEntity;
import com.ltss.entity.auth.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRoleEntity, UserRoleId> {
    @Query("""
            select role.roleCode from UserRoleEntity mapping
            join RoleEntity role on role.id = mapping.id.roleId
            where mapping.id.userId = :userId and mapping.active = true and role.active = true
            order by role.roleCode
            """)
    List<String> findActiveDirectRoleCodes(@Param("userId") Long userId);

    @Query("select count(mapping) from UserRoleEntity mapping where mapping.id.userId = :userId and mapping.active = true")
    long countActiveByUserId(@Param("userId") Long userId);
}
