package com.ltss.repository.auth;

import com.ltss.entity.auth.AccountTokenEntity;
import com.ltss.entity.auth.AccountTokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.time.Instant;
import java.util.Optional;

public interface AccountTokenRepository extends JpaRepository<AccountTokenEntity, Long> {
    Optional<AccountTokenEntity> findByTokenHashAndTokenType(String tokenHash, AccountTokenType tokenType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AccountTokenEntity> findTopByUserIdAndTokenTypeOrderByCreatedAtDesc(
            Long userId,
            AccountTokenType tokenType
    );

    @Modifying
    @Query("""
            update AccountTokenEntity token
            set token.revokedAt = :now
            where token.userId = :userId
              and token.tokenType = :tokenType
              and token.usedAt is null
              and token.revokedAt is null
            """)
    int revokeActiveTokens(
            @Param("userId") Long userId,
            @Param("tokenType") AccountTokenType tokenType,
            @Param("now") Instant now
    );
}
