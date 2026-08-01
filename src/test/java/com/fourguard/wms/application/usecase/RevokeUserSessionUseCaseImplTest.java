package com.fourguard.wms.application.usecase;

import com.fourguard.wms.domain.exception.EntityNotFoundException;
import com.fourguard.wms.domain.ports.out.TokenBlacklistPort;
import com.fourguard.wms.domain.ports.out.UserRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.OrganizationEntity;
import com.fourguard.wms.infrastructure.persistence.entity.PermissionEntity;
import com.fourguard.wms.infrastructure.persistence.entity.RoleEntity;
import com.fourguard.wms.infrastructure.persistence.entity.UserEntity;
import com.fourguard.wms.shared.audit.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.security.Principal;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RevokeUserSessionUseCaseImplTest {

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @Mock
    private TokenBlacklistPort tokenBlacklistPort;

    @Mock
    private AuditService auditService;

    @Mock
    private Principal principal;

    @InjectMocks
    private RevokeUserSessionUseCaseImpl revokeUserSessionUseCase;

    private UserEntity adminUser;
    private UserEntity targetUser;
    private UUID orgId;
    private UUID targetUserId;

    @BeforeEach
    void setUp() {
        orgId = UUID.randomUUID();
        targetUserId = UUID.randomUUID();

        OrganizationEntity org = OrganizationEntity.builder()
                .id(orgId)
                .name("Test Org")
                .code("TORG")
                .build();

        PermissionEntity auditWritePermission = PermissionEntity.builder()
                .id(UUID.randomUUID())
                .name("AUDIT_WRITE")
                .build();

        RoleEntity adminRole = RoleEntity.builder()
                .id(UUID.randomUUID())
                .name("ADMIN")
                .permissions(Set.of(auditWritePermission))
                .build();

        RoleEntity operatorRole = RoleEntity.builder()
                .id(UUID.randomUUID())
                .name("OPERATOR")
                .permissions(Set.of())
                .build();

        adminUser = UserEntity.builder()
                .id(UUID.randomUUID())
                .username("admin")
                .role(adminRole)
                .organization(org)
                .build();

        targetUser = UserEntity.builder()
                .id(targetUserId)
                .username("operator1")
                .role(operatorRole)
                .organization(org)
                .build();
    }

    @Test
    void whenRevokeUserSession_asAdmin_thenSuccess() {
        when(principal.getName()).thenReturn("admin");
        when(userRepositoryPort.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(userRepositoryPort.findById(targetUserId)).thenReturn(Optional.of(targetUser));

        revokeUserSessionUseCase.revokeUserSession(targetUserId, principal);

        verify(tokenBlacklistPort, times(1)).revokeUserSessions(targetUserId);
        verify(auditService, times(1)).log(eq(targetUser), eq("REVOKE_SESSION"), eq("USER"), eq(targetUserId), any(), any());
    }

    @Test
    void whenRevokeUserSession_withoutPermissions_thenThrowAccessDenied() {
        PermissionEntity readOnlyPermission = PermissionEntity.builder()
                .id(UUID.randomUUID())
                .name("READ_ONLY")
                .build();
        RoleEntity plainRole = RoleEntity.builder()
                .id(UUID.randomUUID())
                .name("USER")
                .permissions(Set.of(readOnlyPermission))
                .build();
        UserEntity plainUser = UserEntity.builder()
                .id(UUID.randomUUID())
                .username("plain")
                .role(plainRole)
                .build();

        when(principal.getName()).thenReturn("plain");
        when(userRepositoryPort.findByUsername("plain")).thenReturn(Optional.of(plainUser));

        assertThrows(AccessDeniedException.class, () ->
                revokeUserSessionUseCase.revokeUserSession(targetUserId, principal));

        verify(tokenBlacklistPort, never()).revokeUserSessions(any());
    }

    @Test
    void whenRevokeUserSession_andTargetNotFound_thenThrowEntityNotFound() {
        when(principal.getName()).thenReturn("admin");
        when(userRepositoryPort.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(userRepositoryPort.findById(targetUserId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
                revokeUserSessionUseCase.revokeUserSession(targetUserId, principal));

        verify(tokenBlacklistPort, never()).revokeUserSessions(any());
    }
}
