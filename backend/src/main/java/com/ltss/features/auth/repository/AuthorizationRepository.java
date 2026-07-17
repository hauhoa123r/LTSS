package com.ltss.features.auth.repository;

import com.ltss.features.auth.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Collection;

public interface AuthorizationRepository extends JpaRepository<RoleEntity, Long> {

    String EFFECTIVE_ROLES = """
            WITH RECURSIVE effective_roles(role_id) AS (
                SELECT ur.role_id
                FROM user_roles ur
                JOIN roles direct_role ON direct_role.id = ur.role_id
                WHERE ur.user_id = :userId
                  AND ur.is_active = TRUE
                  AND direct_role.is_active = TRUE
                UNION DISTINCT
                SELECT inheritance.inherited_role_id
                FROM role_inheritances inheritance
                JOIN effective_roles child ON child.role_id = inheritance.role_id
                JOIN roles inherited_role ON inherited_role.id = inheritance.inherited_role_id
                WHERE inherited_role.is_active = TRUE
            )
            """;

    @Query(value = EFFECTIVE_ROLES + """
            SELECT DISTINCT role.role_code
            FROM effective_roles effective
            JOIN roles role ON role.id = effective.role_id
            ORDER BY role.role_code
            """, nativeQuery = true)
    List<String> findEffectiveRoleCodes(@Param("userId") Long userId);

    @Query(value = EFFECTIVE_ROLES + """
            SELECT DISTINCT permission.permission_code
            FROM effective_roles effective
            JOIN role_permissions mapping ON mapping.role_id = effective.role_id
            JOIN permissions permission ON permission.id = mapping.permission_id
            ORDER BY permission.permission_code
            """, nativeQuery = true)
    List<String> findEffectivePermissionCodes(@Param("userId") Long userId);

    @Query(value = """
            SELECT DISTINCT mapping.user_id
            FROM user_roles mapping
            JOIN roles role ON role.id = mapping.role_id
            JOIN users user_account ON user_account.id = mapping.user_id
            WHERE mapping.is_active = TRUE
              AND role.is_active = TRUE
              AND user_account.status = 'ACTIVE'
              AND role.role_code IN (:roleCodes)
            """, nativeQuery = true)
    List<Long> findActiveUserIdsWithDirectRoles(@Param("roleCodes") Collection<String> roleCodes);
}
