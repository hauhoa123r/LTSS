package com.ltss.repository.auth;

import com.ltss.entity.auth.UserEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.ltss.entity.auth.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmail(String email);
    long countByStatus(UserStatus status);

    @Query("""
            select user from UserEntity user
            where (:status is null or user.status = :status)
              and (:query is null or lower(user.email) like lower(concat('%', :query, '%'))
                or lower(user.fullName) like lower(concat('%', :query, '%'))
                or lower(user.displayName) like lower(concat('%', :query, '%')))
            order by user.id desc
            """)
    Page<UserEntity> searchAdmin(@Param("query") String query, @Param("status") UserStatus status, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from UserEntity user where user.id = :id")
    Optional<UserEntity> findLockedById(@Param("id") Long id);
}
