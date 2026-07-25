package com.fourguard.wms.application.usecase;

import com.fourguard.wms.application.dto.request.CreateRoleRequest;
import com.fourguard.wms.application.dto.request.UpdateRoleRequest;
import com.fourguard.wms.application.dto.response.RoleResponse;
import com.fourguard.wms.application.dto.response.audit.RoleAuditResponse;
import com.fourguard.wms.application.mapper.RoleMapper;
import com.fourguard.wms.domain.exception.EntityNotFoundException;
import com.fourguard.wms.domain.ports.out.AuditLogRepositoryPort;
import com.fourguard.wms.domain.ports.out.PermissionRepositoryPort;
import com.fourguard.wms.domain.ports.out.RoleRepositoryPort;
import com.fourguard.wms.domain.ports.out.UserRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.AuditLogEntity;
import com.fourguard.wms.infrastructure.persistence.entity.PermissionEntity;
import com.fourguard.wms.infrastructure.persistence.entity.RoleEntity;
import com.fourguard.wms.shared.audit.AuditService;
import com.fourguard.wms.shared.audit.SecurityAuditHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleRepositoryPort roleRepositoryPort;
    @Mock
    private PermissionRepositoryPort permissionRepositoryPort;
    @Mock
    private RoleMapper roleMapper;
    @Mock
    private SecurityAuditHelper securityAuditHelper;
    @Mock
    private AuditService auditService;
    @Mock
    private AuditLogRepositoryPort auditLogRepositoryPort;
    @Mock
    private UserRepositoryPort userRepositoryPort;

    @InjectMocks
    private RoleService roleService;

    private UUID roleId;
    private UUID permId;
    private RoleEntity roleEntity;
    private PermissionEntity permEntity;
    private RoleResponse roleResponse;
    private UpdateRoleRequest updateRequest;

    @BeforeEach
    void setUp() {
        roleId = UUID.randomUUID();
        permId = UUID.randomUUID();

        permEntity = PermissionEntity.builder()
                .id(permId)
                .name("INVENTORY_READ")
                .description("Permite leer inventario")
                .build();

        roleEntity = RoleEntity.builder()
                .id(roleId)
                .name("OPERATIONS_MANAGER")
                .level(2)
                .isSystem(false)
                .permissions(new HashSet<>(Set.of(permEntity)))
                .build();

        roleResponse = RoleResponse.builder()
                .id(roleId)
                .name("OPERATIONS_MANAGER")
                .level(2)
                .isSystem(false)
                .build();

        updateRequest = UpdateRoleRequest.builder()
                .id(roleId)
                .name("OPERATIONS_MANAGER_UPDATED")
                .level(2)
                .permissionIds(Set.of(permId))
                .build();
    }

    @Test
    void whenUpdateRole_withValidData_thenSuccess() {
        when(roleRepositoryPort.findById(roleId)).thenReturn(Optional.of(roleEntity));
        when(roleRepositoryPort.findByName("OPERATIONS_MANAGER_UPDATED")).thenReturn(Optional.empty());
        when(permissionRepositoryPort.findAllByIds(Set.of(permId))).thenReturn(Set.of(permEntity));
        when(securityAuditHelper.getCurrentUsername()).thenReturn("admin");
        when(roleRepositoryPort.save(any(RoleEntity.class))).thenReturn(roleEntity);
        when(roleMapper.toResponse(roleEntity)).thenReturn(roleResponse);

        RoleResponse response = roleService.updateRole(updateRequest);

        assertNotNull(response);
        verify(roleRepositoryPort, times(1)).save(any(RoleEntity.class));
    }

    @Test
    void whenAssignPermissions_withValidData_thenSuccess() {
        when(roleRepositoryPort.findByIdWithPermissions(roleId)).thenReturn(Optional.of(roleEntity));
        when(permissionRepositoryPort.findAllByIds(Set.of(permId))).thenReturn(Set.of(permEntity));
        when(securityAuditHelper.getCurrentUsername()).thenReturn("admin");
        when(roleRepositoryPort.save(any(RoleEntity.class))).thenReturn(roleEntity);
        when(roleMapper.toResponse(roleEntity)).thenReturn(roleResponse);

        RoleResponse response = roleService.assignPermissions(roleId, Set.of(permId));

        assertNotNull(response);
        verify(roleRepositoryPort, times(1)).save(any(RoleEntity.class));
    }

    @Test
    void whenGetRoleAuditLogs_withExistingId_thenReturnLogs() {
        when(roleRepositoryPort.findById(roleId)).thenReturn(Optional.of(roleEntity));
        AuditLogEntity logEntity = AuditLogEntity.builder()
                .logId(UUID.randomUUID())
                .action("ROLE_PERMISSIONS_ASSIGNED")
                .entityId(roleId)
                .details(List.of())
                .build();
        when(auditLogRepositoryPort.findByEntityTypeAndEntityId("ROLE", roleId)).thenReturn(List.of(logEntity));

        List<RoleAuditResponse> logs = roleService.getRoleAuditLogs(roleId);

        assertNotNull(logs);
        assertEquals(1, logs.size());
        assertEquals("ROLE_PERMISSIONS_ASSIGNED", logs.get(0).getAction());
        verify(auditLogRepositoryPort, times(1)).findByEntityTypeAndEntityId("ROLE", roleId);
    }
}
