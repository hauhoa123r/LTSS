package com.ltss.service.auth;

import com.ltss.entity.auth.AccountTokenEntity;
import com.ltss.entity.auth.AccountTokenType;
import java.time.Duration;

public interface AccountTokenService {

    String issueSecurityToken(
            Long userId,
            AccountTokenType type,
            String ipAddress,
            Duration lifetime,
            boolean otp
    );

    String issueRefreshToken(Long userId, String ipAddress);

    AccountTokenEntity consume(String rawToken, AccountTokenType type);

    AccountTokenEntity consumeOtp(Long userId, String rawOtp);

    AccountTokenEntity rotateRefreshToken(String rawToken);

    void revokeRefreshToken(String rawToken);

    Long findRefreshTokenUserId(String rawToken);

    void revokeAllRefreshTokens(Long userId);
}
