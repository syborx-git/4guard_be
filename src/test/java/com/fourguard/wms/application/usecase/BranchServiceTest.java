package com.fourguard.wms.application.usecase;

import com.fourguard.wms.application.dto.request.CreateBranchRequest;
import com.fourguard.wms.application.dto.request.UpdateBranchRequest;
import com.fourguard.wms.application.dto.response.BranchResponse;
import com.fourguard.wms.application.dto.response.audit.BranchAuditResponse;
import com.fourguard.wms.application.mapper.BranchMapper;
import com.fourguard.wms.domain.enums.BranchStatus;
import com.fourguard.wms.domain.exception.ValidationException;
import com.fourguard.wms.domain.ports.out.AuditLogRepositoryPort;
import com.fourguard.wms.domain.ports.out.BranchRepositoryPort;
import com.fourguard.wms.domain.ports.out.OrganizationRepositoryPort;
import com.fourguard.wms.domain.ports.out.UserRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.AuditLogEntity;
import com.fourguard.wms.infrastructure.persistence.entity.BranchEntity;
import com.fourguard.wms.infrastructure.persistence.entity.OrganizationEntity;
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
class BranchServiceTest {

    @Mock
    private BranchRepositoryPort branchRepositoryPort;
    @Mock
    private OrganizationRepositoryPort organizationRepositoryPort;
    @Mock
    private UserRepositoryPort userRepositoryPort;
    @Mock
    private AuditLogRepositoryPort auditLogRepositoryPort;
    @Mock
    private BranchMapper branchMapper;
    @Mock
    private SecurityAuditHelper securityAuditHelper;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private BranchService branchService;

    private UUID orgId;
    private UUID branchId;
    private OrganizationEntity orgEntity;
    private BranchEntity branchEntity;
    private BranchResponse branchResponse;
    private CreateBranchRequest createRequest;
    private UpdateBranchRequest updateRequest;

    @BeforeEach
    void setUp() {
        orgId = UUID.randomUUID();
        branchId = UUID.randomUUID();

        orgEntity = OrganizationEntity.builder()
                .id(orgId)
                .name("Acme Corp")
                .code("ACME")
                .build();

        branchEntity = BranchEntity.builder()
                .id(branchId)
                .organization(orgEntity)
                .name("Sucursal Norte")
                .code("SUC-NORTE")
                .timezone("UTC")
                .status(BranchStatus.ACTIVE)
                .build();

        branchResponse = BranchResponse.builder()
                .id(branchId)
                .organizationId(orgId)
                .name("Sucursal Norte")
                .code("SUC-NORTE")
                .timezone("UTC")
                .status(BranchStatus.ACTIVE)
                .build();

        createRequest = CreateBranchRequest.builder()
                .organizationId(orgId)
                .name("Sucursal Norte")
                .code("SUC-NORTE")
                .timezone("UTC")
                .build();

        updateRequest = UpdateBranchRequest.builder()
                .id(branchId)
                .organizationId(orgId)
                .name("Sucursal Norte Actualizada")
                .code("SUC-NORTE")
                .timezone("UTC")
                .status(BranchStatus.ACTIVE)
                .build();
    }

    @Test
    void whenCreateBranch_withValidData_thenSuccess() {
        when(organizationRepositoryPort.findById(orgId)).thenReturn(Optional.of(orgEntity));
        when(branchRepositoryPort.existsByOrganizationIdAndCode(orgId, "SUC-NORTE")).thenReturn(false);
        when(branchMapper.toEntity(createRequest)).thenReturn(branchEntity);
        when(securityAuditHelper.getCurrentUsername()).thenReturn("admin");
        when(branchRepositoryPort.save(any(BranchEntity.class))).thenReturn(branchEntity);
        when(branchMapper.toResponse(branchEntity)).thenReturn(branchResponse);

        BranchResponse response = branchService.createBranch(createRequest);

        assertNotNull(response);
        assertEquals("SUC-NORTE", response.getCode());
        verify(branchRepositoryPort, times(1)).save(any(BranchEntity.class));
    }

    @Test
    void whenCreateBranch_withDuplicateCode_thenThrowValidationException() {
        when(organizationRepositoryPort.findById(orgId)).thenReturn(Optional.of(orgEntity));
        when(branchRepositoryPort.existsByOrganizationIdAndCode(orgId, "SUC-NORTE")).thenReturn(true);

        assertThrows(ValidationException.class, () -> branchService.createBranch(createRequest));
        verify(branchRepositoryPort, never()).save(any());
    }

    @Test
    void whenUpdateBranch_withValidData_thenSuccess() {
        when(branchRepositoryPort.findById(branchId)).thenReturn(Optional.of(branchEntity));
        when(organizationRepositoryPort.findById(orgId)).thenReturn(Optional.of(orgEntity));
        when(securityAuditHelper.getCurrentUsername()).thenReturn("admin");
        when(branchRepositoryPort.save(any(BranchEntity.class))).thenReturn(branchEntity);
        when(branchMapper.toResponse(branchEntity)).thenReturn(branchResponse);

        BranchResponse response = branchService.updateBranch(updateRequest);

        assertNotNull(response);
        verify(branchRepositoryPort, times(1)).save(any(BranchEntity.class));
    }

    @Test
    void whenDeleteBranch_withExistingId_thenSuccess() {
        when(branchRepositoryPort.findById(branchId)).thenReturn(Optional.of(branchEntity));
        when(securityAuditHelper.getCurrentUsername()).thenReturn("admin");

        branchService.deleteBranch(branchId);

        verify(branchRepositoryPort, times(1)).deleteById(branchId);
    }

    @Test
    void whenGetBranchAuditLogs_withExistingId_thenReturnLogs() {
        when(branchRepositoryPort.findById(branchId)).thenReturn(Optional.of(branchEntity));
        AuditLogEntity logEntity = AuditLogEntity.builder()
                .logId(UUID.randomUUID())
                .action("BRANCH_CREATED")
                .entityId(branchId)
                .details(List.of())
                .build();
        when(auditLogRepositoryPort.findByEntityTypeAndEntityId("BRANCH", branchId)).thenReturn(List.of(logEntity));

        List<BranchAuditResponse> logs = branchService.getBranchAuditLogs(branchId);

        assertNotNull(logs);
        assertEquals(1, logs.size());
        assertEquals("BRANCH_CREATED", logs.get(0).getAction());
        verify(auditLogRepositoryPort, times(1)).findByEntityTypeAndEntityId("BRANCH", branchId);
    }
}
