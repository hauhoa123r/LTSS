package com.ltss.features.administration.service;

import com.ltss.common.exception.ConflictException;
import com.ltss.features.administration.dto.*;
import com.ltss.features.auth.entity.*;
import com.ltss.features.auth.repository.*;
import com.ltss.features.auth.security.CurrentUserService;
import com.ltss.features.auth.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdministrationServiceTest {
    private static final Instant NOW = Instant.parse("2026-07-16T09:00:00Z");
    private static final ClientRequestInfo REQUEST = new ClientRequestInfo("127.0.0.1", "req-1");
    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock UserRoleRepository userRoleRepository;
    @Mock AuthorizationRepository authorizationRepository;
    @Mock CurrentUserService currentUserService;
    @Mock AccountTokenService tokenService;
    @Mock AuditService auditService;
    AdministrationService service;

    @BeforeEach
    void setUp() {
        service = new AdministrationService(userRepository, roleRepository, userRoleRepository,
                authorizationRepository, currentUserService, tokenService, auditService,
                Clock.fixed(NOW, ZoneOffset.UTC));
        lenient().when(currentUserService.requireUserId()).thenReturn(10L);
    }

    @Test
    void administratorCannotChangeOwnStatus() {
        allowAdmin();
        assertThrows(ConflictException.class, () -> service.changeStatus(10L,
                new ChangeUserStatusRequest(UserStatus.SUSPENDED, "Bảo vệ tài khoản", 0), REQUEST));
        verify(userRepository, never()).findLockedById(anyLong());
    }

    @Test
    void nonAdministratorCannotListUsers() {
        when(authorizationRepository.findEffectiveRoleCodes(10L)).thenReturn(List.of("MODERATOR"));
        assertThrows(AccessDeniedException.class, () -> service.users(null, null, 0, 20));
    }

    @Test
    void statusChangeRevokesSessionsAndWritesSafeAudit() {
        allowAdmin();
        UserEntity target = new UserEntity("Nguyễn Văn A", "A", "a@example.com", "hash");
        ReflectionTestUtils.setField(target, "id", 20L);
        target.verifyEmail(NOW.minusSeconds(100));
        when(userRepository.findLockedById(20L)).thenReturn(Optional.of(target));
        when(userRoleRepository.findActiveDirectRoleCodes(20L)).thenReturn(List.of("TOURIST"));
        when(authorizationRepository.findEffectiveRoleCodes(20L)).thenReturn(List.of("TOURIST"));

        AdminUserResponse response = service.changeStatus(20L,
                new ChangeUserStatusRequest(UserStatus.SUSPENDED, "Vi phạm chính sách", 0), REQUEST);

        assertEquals(UserStatus.SUSPENDED, response.status());
        verify(tokenService).revokeAllRefreshTokens(20L);
        verify(auditService).recordDomainChange(eq(10L), eq("ADMIN_USER_STATUS_CHANGED"),
                eq("USER"), eq(20L), anyMap(), anyMap(), eq(REQUEST));
    }

    @Test
    void lastDirectRoleCannotBeRevoked() {
        allowAdmin();
        UserEntity target = new UserEntity("Nguyễn Văn A", "A", "a@example.com", "hash");
        ReflectionTestUtils.setField(target, "id", 20L);
        when(userRepository.findById(20L)).thenReturn(Optional.of(target));
        RoleEntity role = mock(RoleEntity.class);
        when(role.getId()).thenReturn(1L);
        when(roleRepository.findByRoleCodeAndActiveTrue("TOURIST")).thenReturn(Optional.of(role));
        UserRoleEntity mapping = mock(UserRoleEntity.class);
        when(mapping.isActive()).thenReturn(true);
        when(userRoleRepository.findById(any())).thenReturn(Optional.of(mapping));
        when(userRoleRepository.countActiveByUserId(20L)).thenReturn(1L);

        assertThrows(ConflictException.class, () -> service.revokeRole(
                20L, "TOURIST", new RoleChangeRequest("Điều chỉnh"), REQUEST));
        verify(mapping, never()).revoke(anyLong(), any());
    }

    private void allowAdmin() {
        when(authorizationRepository.findEffectiveRoleCodes(10L)).thenReturn(List.of("ADMINISTRATOR"));
    }
}
