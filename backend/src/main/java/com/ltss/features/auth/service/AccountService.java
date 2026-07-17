package com.ltss.features.auth.service;

import com.ltss.features.auth.config.AccountProperties;
import com.ltss.features.auth.dto.request.ChangePasswordRequest;
import com.ltss.features.auth.dto.request.UpdateProfileRequest;
import com.ltss.features.auth.dto.response.MessageResponse;
import com.ltss.features.auth.dto.response.ProfileResponse;
import com.ltss.features.auth.email.AccountEmailEvent;
import com.ltss.features.auth.email.AccountEmailType;
import com.ltss.features.auth.entity.AccountTokenEntity;
import com.ltss.features.auth.entity.AccountTokenType;
import com.ltss.features.auth.entity.PasswordChangeReason;
import com.ltss.features.auth.entity.PasswordHistoryEntity;
import com.ltss.features.auth.entity.UserEntity;
import com.ltss.features.auth.exception.AccountException;
import com.ltss.features.auth.repository.AuthorizationRepository;
import com.ltss.features.auth.repository.PasswordHistoryRepository;
import com.ltss.features.auth.repository.UserRepository;
import com.ltss.features.auth.security.CurrentUserService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

@Service
public class AccountService {
    private final UserRepository userRepository;
    private final AuthorizationRepository authorizationRepository;
    private final PasswordHistoryRepository passwordHistoryRepository;
    private final CurrentUserService currentUserService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final AccountTokenService tokenService;
    private final AuditService auditService;
    private final AccountProperties properties;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public AccountService(
            UserRepository userRepository,
            AuthorizationRepository authorizationRepository,
            PasswordHistoryRepository passwordHistoryRepository,
            CurrentUserService currentUserService,
            PasswordEncoder passwordEncoder,
            PasswordPolicy passwordPolicy,
            AccountTokenService tokenService,
            AuditService auditService,
            AccountProperties properties,
            ApplicationEventPublisher eventPublisher,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.authorizationRepository = authorizationRepository;
        this.passwordHistoryRepository = passwordHistoryRepository;
        this.currentUserService = currentUserService;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicy = passwordPolicy;
        this.tokenService = tokenService;
        this.auditService = auditService;
        this.properties = properties;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile() {
        UserEntity user = currentUser();
        return toProfile(user);
    }

    @Transactional
    public ProfileResponse updateProfile(UpdateProfileRequest request, ClientRequestInfo requestInfo) {
        UserEntity user = currentUser();
        user.updateProfile(
                request.fullName().trim(),
                request.displayName().trim(),
                normalizeNullable(request.phone()),
                normalizeNullable(request.address())
        );
        auditService.record(user.getId(), "PROFILE_UPDATED", user.getId(), requestInfo);
        return toProfile(user);
    }

    @Transactional
    public MessageResponse requestChangePasswordOtp(ClientRequestInfo requestInfo) {
        UserEntity user = currentUser();
        String otp = tokenService.issueSecurityToken(
                user.getId(),
                AccountTokenType.CHANGE_PASSWORD_OTP,
                requestInfo.ipAddress(),
                properties.securityTokenLifetime(),
                true
        );
        eventPublisher.publishEvent(new AccountEmailEvent(
                user.getEmail(), AccountEmailType.CHANGE_PASSWORD_OTP, otp
        ));
        return new MessageResponse("A password-change code was sent to your email");
    }

    @Transactional
    public MessageResponse changePassword(
            ChangePasswordRequest request,
            ClientRequestInfo requestInfo
    ) {
        UserEntity user = currentUser();
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw AccountException.invalidCredentials();
        }

        tokenService.consumeOtp(user.getId(), request.otp());

        passwordPolicy.validateForIdentity(
                request.newPassword(), user.getEmail(), user.getFullName(), user.getDisplayName()
        );
        List<PasswordHistoryEntity> history =
                passwordHistoryRepository.findTop4ByUserIdOrderByCreatedAtDesc(user.getId());
        passwordPolicy.validateNotReused(request.newPassword(), user.getPasswordHash(), history);

        String encoded = passwordEncoder.encode(request.newPassword());
        user.changePassword(encoded, clock.instant());
        passwordHistoryRepository.save(new PasswordHistoryEntity(
                user.getId(), encoded, PasswordChangeReason.USER_CHANGE, clock.instant()
        ));
        tokenService.revokeAllRefreshTokens(user.getId());
        auditService.record(user.getId(), "PASSWORD_CHANGED", user.getId(), requestInfo);
        return new MessageResponse("Password changed successfully. Sign in again on every device");
    }

    private UserEntity currentUser() {
        return userRepository.findById(currentUserService.requireUserId())
                .orElseThrow(AccountException::forbiddenState);
    }

    private ProfileResponse toProfile(UserEntity user) {
        return ProfileResponse.from(
                user,
                authorizationRepository.findEffectiveRoleCodes(user.getId()),
                authorizationRepository.findEffectivePermissionCodes(user.getId())
        );
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
