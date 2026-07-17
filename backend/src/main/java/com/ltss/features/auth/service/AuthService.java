package com.ltss.features.auth.service;

import com.ltss.features.auth.config.AccountProperties;
import com.ltss.features.auth.dto.request.LoginRequest;
import com.ltss.features.auth.dto.request.RegisterRequest;
import com.ltss.features.auth.dto.request.ResetPasswordRequest;
import com.ltss.features.auth.dto.response.AuthResponse;
import com.ltss.features.auth.dto.response.MessageResponse;
import com.ltss.features.auth.dto.response.ProfileResponse;
import com.ltss.features.auth.email.AccountEmailEvent;
import com.ltss.features.auth.email.AccountEmailType;
import com.ltss.features.auth.entity.AccountTokenEntity;
import com.ltss.features.auth.entity.AccountTokenType;
import com.ltss.features.auth.entity.PasswordChangeReason;
import com.ltss.features.auth.entity.PasswordHistoryEntity;
import com.ltss.features.auth.entity.RoleEntity;
import com.ltss.features.auth.entity.UserEntity;
import com.ltss.features.auth.entity.UserRoleEntity;
import com.ltss.features.auth.entity.UserStatus;
import com.ltss.features.auth.exception.AccountException;
import com.ltss.features.auth.repository.AccountTokenRepository;
import com.ltss.features.auth.repository.AuthorizationRepository;
import com.ltss.features.auth.repository.PasswordHistoryRepository;
import com.ltss.features.auth.repository.RoleRepository;
import com.ltss.features.auth.repository.UserRepository;
import com.ltss.features.auth.repository.UserRoleRepository;
import com.ltss.features.auth.security.IssuedAccessToken;
import com.ltss.features.auth.security.JwtService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
public class AuthService {
    private static final String TOURIST_ROLE = "TOURIST";
    private static final String GENERIC_EMAIL_MESSAGE =
            "If the account is eligible, an email will be sent shortly";
    private static final String DUMMY_PASSWORD_HASH =
            "$2a$10$7EqJtq98hPqEX7fNZaFWoO5E7Hh7U/K6p01q9mS8YlPzYx8kVQW0K";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordHistoryRepository passwordHistoryRepository;
    private final AuthorizationRepository authorizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final AccountTokenService tokenService;
    private final JwtService jwtService;
    private final AuditService auditService;
    private final AccountProperties properties;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            PasswordHistoryRepository passwordHistoryRepository,
            AuthorizationRepository authorizationRepository,
            PasswordEncoder passwordEncoder,
            PasswordPolicy passwordPolicy,
            AccountTokenService tokenService,
            JwtService jwtService,
            AuditService auditService,
            AccountProperties properties,
            ApplicationEventPublisher eventPublisher,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordHistoryRepository = passwordHistoryRepository;
        this.authorizationRepository = authorizationRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
        this.tokenService = tokenService;
        this.jwtService = jwtService;
        this.auditService = auditService;
        this.properties = properties;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public MessageResponse register(RegisterRequest request, ClientRequestInfo requestInfo) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw AccountException.conflict("An account with this email already exists");
        }

        String fullName = request.fullName().trim();
        String displayName = request.displayName().trim();
        passwordPolicy.validateForIdentity(request.password(), email, fullName, displayName);
        String encodedPassword = passwordEncoder.encode(request.password());

        UserEntity user = userRepository.save(new UserEntity(
                fullName,
                displayName,
                email,
                encodedPassword
        ));
        RoleEntity touristRole = roleRepository.findByRoleCodeAndActiveTrue(TOURIST_ROLE)
                .orElseThrow(() -> new IllegalStateException("TOURIST role is not configured"));
        userRoleRepository.save(new UserRoleEntity(user.getId(), touristRole.getId()));
        passwordHistoryRepository.save(new PasswordHistoryEntity(
                user.getId(),
                encodedPassword,
                PasswordChangeReason.REGISTRATION,
                clock.instant()
        ));

        String rawToken = tokenService.issueSecurityToken(
                user.getId(),
                AccountTokenType.EMAIL_VERIFICATION,
                requestInfo.ipAddress(),
                properties.verificationTokenLifetime(),
                false
        );
        auditService.record(user.getId(), "ACCOUNT_REGISTERED", user.getId(), requestInfo);
        publishEmail(user.getEmail(), AccountEmailType.VERIFY_EMAIL, rawToken);
        return new MessageResponse("Registration succeeded. Verify your email before signing in");
    }

    @Transactional
    public MessageResponse verifyEmail(String rawToken, ClientRequestInfo requestInfo) {
        AccountTokenEntity token = tokenService.consume(rawToken, AccountTokenType.EMAIL_VERIFICATION);
        UserEntity user = requireUser(token.getUserId());
        if (user.getStatus() != UserStatus.PENDING_VERIFICATION) {
            throw AccountException.invalidToken();
        }
        user.verifyEmail(clock.instant());
        auditService.record(user.getId(), "EMAIL_VERIFIED", user.getId(), requestInfo);
        return new MessageResponse("Email verified successfully");
    }

    @Transactional
    public MessageResponse resendVerification(String rawEmail, ClientRequestInfo requestInfo) {
        userRepository.findByEmail(normalizeEmail(rawEmail))
                .filter(user -> user.getStatus() == UserStatus.PENDING_VERIFICATION)
                .ifPresent(user -> issuePublicEmailSafely(
                        user,
                        AccountTokenType.EMAIL_VERIFICATION,
                        AccountEmailType.VERIFY_EMAIL,
                        properties.verificationTokenLifetime(),
                        requestInfo,
                        false
                ));
        return new MessageResponse(GENERIC_EMAIL_MESSAGE);
    }

    @Transactional(noRollbackFor = AccountException.class)
    public AuthenticatedSession login(LoginRequest request, ClientRequestInfo requestInfo) {
        String email = normalizeEmail(request.email());
        UserEntity user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            passwordEncoder.matches(request.password(), DUMMY_PASSWORD_HASH);
            throw AccountException.invalidCredentials();
        }

        Instant now = clock.instant();
        boolean locked = user.getLockedUntil() != null && user.getLockedUntil().isAfter(now);
        boolean passwordMatches = passwordEncoder.matches(request.password(), user.getPasswordHash());
        if (locked || !passwordMatches || user.getStatus() != UserStatus.ACTIVE) {
            if (!locked && !passwordMatches && user.getStatus() == UserStatus.ACTIVE) {
                user.recordFailedLogin(
                        properties.maxLoginFailures(),
                        now.plus(properties.temporaryLockDuration())
                );
            }
            auditService.record(user.getId(), "LOGIN_FAILED", user.getId(), requestInfo);
            throw AccountException.invalidCredentials();
        }

        user.recordSuccessfulLogin(now);
        auditService.record(user.getId(), "LOGIN_SUCCEEDED", user.getId(), requestInfo);
        return createSession(user, requestInfo.ipAddress());
    }

    @Transactional(noRollbackFor = AccountException.class)
    public AuthenticatedSession refresh(String rawRefreshToken, ClientRequestInfo requestInfo) {
        AccountTokenEntity oldToken = tokenService.rotateRefreshToken(rawRefreshToken);
        UserEntity user = requireActiveUser(oldToken.getUserId());
        auditService.record(user.getId(), "SESSION_REFRESHED", user.getId(), requestInfo);
        return createSession(user, requestInfo.ipAddress());
    }

    @Transactional
    public void logout(String rawRefreshToken, ClientRequestInfo requestInfo) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        Long userId = tokenService.findRefreshTokenUserId(rawRefreshToken);
        if (userId != null) {
            auditService.record(
                        userId,
                        "LOGOUT",
                        userId,
                        requestInfo
                );
        }
        tokenService.revokeRefreshToken(rawRefreshToken);
    }

    @Transactional
    public MessageResponse forgotPassword(String rawEmail, ClientRequestInfo requestInfo) {
        userRepository.findByEmail(normalizeEmail(rawEmail))
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .ifPresent(user -> issuePublicEmailSafely(
                        user,
                        AccountTokenType.PASSWORD_RESET,
                        AccountEmailType.RESET_PASSWORD,
                        properties.securityTokenLifetime(),
                        requestInfo,
                        false
                ));
        return new MessageResponse(GENERIC_EMAIL_MESSAGE);
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request, ClientRequestInfo requestInfo) {
        AccountTokenEntity token = tokenService.consume(request.token(), AccountTokenType.PASSWORD_RESET);
        UserEntity user = requireActiveUser(token.getUserId());
        changePassword(user, request.newPassword(), PasswordChangeReason.PASSWORD_RESET);
        tokenService.revokeAllRefreshTokens(user.getId());
        auditService.record(user.getId(), "PASSWORD_RESET", user.getId(), requestInfo);
        return new MessageResponse("Password reset successfully. Sign in again with the new password");
    }

    private AuthenticatedSession createSession(UserEntity user, String ipAddress) {
        List<String> roles = authorizationRepository.findEffectiveRoleCodes(user.getId());
        List<String> permissions = authorizationRepository.findEffectivePermissionCodes(user.getId());
        IssuedAccessToken accessToken = jwtService.issue(user, roles, permissions);
        String refreshToken = tokenService.issueRefreshToken(user.getId(), ipAddress);
        ProfileResponse profile = ProfileResponse.from(user, roles, permissions);
        return new AuthenticatedSession(
                new AuthResponse(accessToken.value(), "Bearer", accessToken.expiresAt(), profile),
                refreshToken
        );
    }

    private void changePassword(UserEntity user, String newPassword, PasswordChangeReason reason) {
        passwordPolicy.validateForIdentity(
                newPassword,
                user.getEmail(),
                user.getFullName(),
                user.getDisplayName()
        );
        passwordPolicy.validateNotReused(
                newPassword,
                user.getPasswordHash(),
                passwordHistoryRepository.findTop4ByUserIdOrderByCreatedAtDesc(user.getId())
        );
        String encoded = passwordEncoder.encode(newPassword);
        user.changePassword(encoded, clock.instant());
        passwordHistoryRepository.save(new PasswordHistoryEntity(
                user.getId(), encoded, reason, clock.instant()
        ));
    }

    private void issuePublicEmailSafely(
            UserEntity user,
            AccountTokenType tokenType,
            AccountEmailType emailType,
            java.time.Duration lifetime,
            ClientRequestInfo requestInfo,
            boolean otp
    ) {
        try {
            String token = tokenService.issueSecurityToken(
                    user.getId(), tokenType, requestInfo.ipAddress(), lifetime, otp
            );
            publishEmail(user.getEmail(), emailType, token);
        } catch (AccountException exception) {
            if (!"TOKEN_REQUEST_RATE_LIMITED".equals(exception.getCode())) {
                throw exception;
            }
        }
    }

    private void publishEmail(String email, AccountEmailType type, String token) {
        eventPublisher.publishEvent(new AccountEmailEvent(email, type, token));
    }

    private UserEntity requireUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(AccountException::invalidToken);
    }

    private UserEntity requireActiveUser(Long userId) {
        UserEntity user = requireUser(userId);
        Instant now = clock.instant();
        if (user.getStatus() != UserStatus.ACTIVE
                || (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now))) {
            throw AccountException.forbiddenState();
        }
        return user;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
