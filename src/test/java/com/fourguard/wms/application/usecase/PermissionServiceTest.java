package com.fourguard.wms.application.usecase;

import com.fourguard.wms.application.dto.request.CreatePermissionRequest;
import com.fourguard.wms.application.dto.response.PermissionResponse;
import com.fourguard.wms.application.dto.response.audit.PermissionAuditResponse;
import com.fourguard.wms.application.mapper.PermissionMapper;
import com.fourguard.wms.domain.exception.EntityNotFoundException;
import com.fourguard.wms.domain.ports.out.AuditLogRepositoryPort;
import com.fourguard.wms.domain.ports.out.PermissionRepositoryPort;
import com.fourguard.wms.domain.ports.out.UserRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.AuditLogEntity;
import com.fourguard.wms.infrastructure.persistence.entity.PermissionEntity;
import com.fourguard.wms.shared.audit.AuditService;
import com.fourguard.wms.shared.audit.SecurityAuditHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock
    private PermissionRepositoryPort permissionRepositoryPort;
    @Mock
    private PermissionMapper permissionMapper;
    @Mock
    private SecurityAuditHelper securityAuditHelper;
    @Mock
    private AuditService auditService;
    @Mock
    private AuditLogRepositoryPort auditLogRepositoryPort;
    @Mock
    private UserRepositoryPort userRepositoryPort;

    @InjectMocks
    private PermissionService permissionService;

    private UUID permId;
    private PermissionEntity permEntity;
    private PermissionResponse permResponse;
    private CreatePermissionRequest createRequest;

    @BeforeEach
    void setUp() {
        permId = UUID.randomUUID();

        permEntity = PermissionEntity.builder()
                .id(permId)
                .name("INVENTORY_READ")
                .description("Permite leer inventario")
                .build();

        permResponse = PermissionResponse.builder()
                .id(permId)
                .name("INVENTORY_READ")
                .description("Permite leer inventario")
                .build();

        createRequest = CreatePermissionRequest.builder()
                .name("INVENTORY_READ")
                .description("Permite leer inventario")
                .build();
    }

    @Test
    void whenCreatePermission_withValidData_thenSuccess() {
        when(permissionRepositoryPort.existsByName("INVENTORY_READ")).thenReturn(false);
        when(permissionMapper.toEntity(createRequest)).thenReturn(permEntity);
        when(securityAuditHelper.getCurrentUsername()).thenReturn("admin");
        when(permissionRepositoryPort.save(any(PermissionEntity.class))).thenReturn(permEntity);
        when(permissionMapper.toResponse(permEntity)).thenReturn(permResponse);

        PermissionResponse response = permissionService.createPermission(createRequest);

        assertNotNull(response);
        assertEquals("INVENTORY_READ", response.getName());
        verify(permissionRepositoryPort, times(1)).save(any(PermissionEntity.class));
    }

    @Test
    void whenGetPermissionAuditLogs_withExistingId_thenReturnLogs() {
        when(permissionRepositoryPort.findById(permId)).thenReturn(Optional.of(permEntity));
        AuditLogEntity logEntity = AuditLogEntity.builder()
                .logId(UUID.randomUUID())
                .action("PERMISSION_CREATED")
                .entityId(permId)
                .details(List.of())
                .build();
        when(auditLogRepositoryPort.findByEntityTypeAndEntityId("PERMISSION", permId)).thenReturn(List.of(logEntity));

        List<PermissionAuditResponse> logs = permissionService.getPermissionAuditLogs(permId);

        assertNotNull(logs);
        assertEquals(1, logs.size());
        assertEquals("PERMISSION_CREATED", logs.get(0).getAction());
        verify(auditLogRepositoryPort, times(1)).findByEntityTypeAndEntityId("PERMISSION", permId);
    }
}
