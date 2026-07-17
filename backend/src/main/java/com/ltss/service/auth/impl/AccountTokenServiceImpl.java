package com.ltss.service.auth.impl;

import com.ltss.service.auth.AccountTokenService;
import com.ltss.service.auth.TokenHashService;

import com.ltss.config.auth.AccountProperties;
import com.ltss.entity.auth.AccountTokenEntity;
import com.ltss.entity.auth.AccountTokenType;
import com.ltss.common.exception.auth.AccountException;
import com.ltss.repository.auth.AccountTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
public class AccountTokenServiceImpl implements AccountTokenService {
    private final AccountTokenRepository tokenRepository;
    private final TokenHashService tokenHashService;
    private final AccountProperties properties;
    private final Clock clock;
    private final PasswordEncoder passwordEncoder;

    public AccountTokenServiceImpl(
            AccountTokenRepository tokenRepository,
            TokenHashService tokenHashService,
            AccountProperties properties,
            Clock clock,
            PasswordEncoder passwordEncoder
    ) {
        this.tokenRepository = tokenRepository;
        this.tokenHashService = tokenHashService;
        this.properties = properties;
        this.clock = clock;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String issueSecurityToken(
            Long userId,
            AccountTokenType type,
            String ipAddress,
            Duration lifetime,
            boolean otp
    ) {
        Instant now = clock.instant();
        enforceResendDelay(userId, type, now);
        tokenRepository.revokeActiveTokens(userId, type, now);
        String rawToken = otp ? tokenHashService.generateOtp() : tokenHashService.generateOpaqueToken();
        save(userId, type, rawToken, ipAddress, lifetime, now, otp);
        return rawToken;
    }

    @Override
    public String issueRefreshToken(Long userId, String ipAddress) {
        String rawToken = tokenHashService.generateOpaqueToken();
        save(
                userId,
                AccountTokenType.REFRESH_TOKEN,
                rawToken,
                ipAddress,
                properties.refreshTokenLifetime(),
                clock.instant(),
                false
        );
        return rawToken;
    }

    @Override
    public AccountTokenEntity consume(String rawToken, AccountTokenType type) {
        Instant now = clock.instant();
        AccountTokenEntity token = tokenRepository
                .findByTokenHashAndTokenType(tokenHashService.hash(rawToken), type)
                .filter(candidate -> candidate.isUsableAt(now))
                .orElseThrow(AccountException::invalidToken);
        token.markUsed(now);
        return token;
    }

    @Override
    public AccountTokenEntity consumeOtp(Long userId, String rawOtp) {
        Instant now = clock.instant();
        AccountTokenEntity token = tokenRepository
                .findTopByUserIdAndTokenTypeOrderByCreatedAtDesc(
                        userId, AccountTokenType.CHANGE_PASSWORD_OTP
                )
                .filter(candidate -> candidate.isUsableAt(now))
                .filter(candidate -> passwordEncoder.matches(rawOtp, candidate.getTokenHash()))
                .orElseThrow(AccountException::invalidToken);
        token.markUsed(now);
        return token;
    }

    @Override
    public AccountTokenEntity rotateRefreshToken(String rawToken) {
        AccountTokenEntity token = consume(rawToken, AccountTokenType.REFRESH_TOKEN);
        token.revoke(clock.instant());
        return token;
    }

    @Override
    public void revokeRefreshToken(String rawToken) {
        tokenRepository.findByTokenHashAndTokenType(
                        tokenHashService.hash(rawToken),
                        AccountTokenType.REFRESH_TOKEN
                )
                .filter(token -> token.getRevokedAt() == null)
                .ifPresent(token -> token.revoke(clock.instant()));
    }

    @Override
    public Long findRefreshTokenUserId(String rawToken) {
        return tokenRepository.findByTokenHashAndTokenType(
                        tokenHashService.hash(rawToken),
                        AccountTokenType.REFRESH_TOKEN
                )
                .map(AccountTokenEntity::getUserId)
                .orElse(null);
    }

    @Override
    public void revokeAllRefreshTokens(Long userId) {
        tokenRepository.revokeActiveTokens(userId, AccountTokenType.REFRESH_TOKEN, clock.instant());
    }

    private void enforceResendDelay(Long userId, AccountTokenType type, Instant now) {
        tokenRepository.findTopByUserIdAndTokenTypeOrderByCreatedAtDesc(userId, type)
                .filter(latest -> latest.getCreatedAt().plus(properties.tokenResendDelay()).isAfter(now))
                .ifPresent(latest -> {
                    throw AccountException.rateLimited();
                });
    }

    private void save(
            Long userId,
            AccountTokenType type,
            String rawToken,
            String ipAddress,
            Duration lifetime,
            Instant now,
            boolean passwordHash
    ) {
        tokenRepository.save(new AccountTokenEntity(
                userId,
                type,
                passwordHash ? passwordEncoder.encode(rawToken) : tokenHashService.hash(rawToken),
                now.plus(lifetime),
                ipAddress,
                now
        ));
    }
}
