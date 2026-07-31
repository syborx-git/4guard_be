package com.fourguard.wms.application.usecase;

import com.fourguard.wms.application.dto.response.audit.UserActivityAuditResponse;
import com.fourguard.wms.domain.ports.out.AuditLogRepositoryPort;
import com.fourguard.wms.domain.ports.out.UserRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.AuditLogDetailEntity;
import com.fourguard.wms.infrastructure.persistence.entity.AuditLogEntity;
import com.fourguard.wms.infrastructure.persistence.entity.PermissionEntity;
import com.fourguard.wms.infrastructure.persistence.entity.RoleEntity;
import com.fourguard.wms.infrastructure.persistence.entity.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetUserActivityUseCaseImplTest {

    @Mock
    private AuditLogRepositoryPort auditLogRepositoryPort;

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @Mock
    private Principal principal;

    @InjectMocks
    private GetUserActivityUseCaseImpl getUserActivityUseCase;

    private UserEntity requestingUser;
    private UUID targetUserId;

    @BeforeEach
    void setUp() {
        PermissionEntity auditReadPerm = PermissionEntity.builder().name("AUDIT_READ").build();
        RoleEntity role = RoleEntity.builder()
                .name("ADMIN")
                .permissions(Set.of(auditReadPerm))
                .build();

        requestingUser = UserEntity.builder()
                .id(UUID.randomUUID())
                .username("admin_user")
                .role(role)
                .build();

        targetUserId = UUID.randomUUID();
    }

    @Test
    void whenGetUserActivityWithValidPermissions_returnsMappedLogs() {
        when(principal.getName()).thenReturn("admin_user");
        when(userRepositoryPort.findByUsername("admin_user")).thenReturn(Optional.of(requestingUser));

        AuditLogEntity logEntity = AuditLogEntity.builder()
                .logId(UUID.randomUUID())
                .userId(targetUserId)
                .action("LOGIN")
                .entityType("AUTH")
                .entityId(targetUserId)
                .ipAddress("127.0.0.1")
                .userAgent("Mozilla/5.0")
                .createdAt(OffsetDateTime.now())
                .details(List.of(
                        AuditLogDetailEntity.builder().fieldName("status").oldValue(null).newValue("SUCCESS").build()
                ))
                .build();

        when(auditLogRepositoryPort.findUserActivity(targetUserId, "login", null, null))
                .thenReturn(List.of(logEntity));

        UserEntity targetUser = UserEntity.builder()
                .id(targetUserId)
                .username("target_user")
                .build();

        when(userRepositoryPort.findAllById(List.of(targetUserId))).thenReturn(List.of(targetUser));

        List<UserActivityAuditResponse> result = getUserActivityUseCase.getUserActivityLogs(
                targetUserId, "LOGIN", null, null, principal);

        assertNotNull(result);
        assertEquals(1, result.size());
        UserActivityAuditResponse response = result.get(0);
        assertEquals("LOGIN", response.getAction());
        assertEquals("target_user", response.getUsername());
        assertEquals(1, response.getDetails().size());
        assertEquals("status", response.getDetails().get(0).getFieldName());
    }

    @Test
    void whenUserNotAuthenticated_throwsAccessDeniedException() {
        assertThrows(AccessDeniedException.class, () ->
                getUserActivityUseCase.getUserActivityLogs(null, null, null, null, null));
    }

    @Test
    void whenUserLacksPermissions_throwsAccessDeniedException() {
        RoleEntity restrictedRole = RoleEntity.builder().name("OPERATOR").permissions(Collections.emptySet()).build();
        UserEntity restrictedUser = UserEntity.builder().id(UUID.randomUUID()).username("operator").role(restrictedRole).build();

        when(principal.getName()).thenReturn("operator");
        when(userRepositoryPort.findByUsername("operator")).thenReturn(Optional.of(restrictedUser));

        assertThrows(AccessDeniedException.class, () ->
                getUserActivityUseCase.getUserActivityLogs(null, null, null, null, principal));
    }
}
