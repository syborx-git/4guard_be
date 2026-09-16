package com.fourguard.wms.application.usecase;

import com.fourguard.wms.application.dto.request.CreateOrganizationRequest;
import com.fourguard.wms.application.dto.request.UpdateOrganizationRequest;
import com.fourguard.wms.application.dto.response.OrganizationResponse;
import com.fourguard.wms.application.dto.response.audit.OrganizationAuditResponse;
import com.fourguard.wms.application.mapper.OrganizationMapper;
import com.fourguard.wms.domain.enums.OrganizationStatus;
import com.fourguard.wms.domain.enums.OrganizationType;
import com.fourguard.wms.domain.exception.ValidationException;
import com.fourguard.wms.domain.ports.out.AuditLogRepositoryPort;
import com.fourguard.wms.domain.ports.out.OrganizationRepositoryPort;
import com.fourguard.wms.domain.ports.out.UserRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.AuditLogEntity;
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
class OrganizationServiceTest {

    @Mock
    private OrganizationRepositoryPort organizationRepositoryPort;
    @Mock
    private UserRepositoryPort userRepositoryPort;
    @Mock
    private AuditLogRepositoryPort auditLogRepositoryPort;
    @Mock
    private OrganizationMapper organizationMapper;
    @Mock
    private SecurityAuditHelper securityAuditHelper;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private OrganizationService organizationService;

    private UUID orgId;
    private OrganizationEntity orgEntity;
    private OrganizationResponse orgResponse;
    private CreateOrganizationRequest createRequest;
    private UpdateOrganizationRequest updateRequest;

    @BeforeEach
    void setUp() {
        orgId = UUID.randomUUID();
        orgEntity = OrganizationEntity.builder()
                .id(orgId)
                .name("Acme Corp")
                .code("ACME")
                .taxId("ACME123456")
                .type(OrganizationType.LOGISTICS)
                .status(OrganizationStatus.ACTIVE)
                .build();

        orgResponse = OrganizationResponse.builder()
                .id(orgId)
                .name("Acme Corp")
                .code("ACME")
                .taxId("ACME123456")
                .type(OrganizationType.LOGISTICS)
                .status(OrganizationStatus.ACTIVE)
                .build();

        createRequest = CreateOrganizationRequest.builder()
                .name("Acme Corp")
                .code("ACME")
                .taxId("ACME123456")
                .type(OrganizationType.LOGISTICS)
                .build();

        updateRequest = UpdateOrganizationRequest.builder()
                .id(orgId)
                .name("Acme Corp Updated")
                .taxId("ACME123456")
                .type(OrganizationType.LOGISTICS)
                .status(OrganizationStatus.ACTIVE)
                .build();
    }

    @Test
    void whenCreateOrganization_withValidData_thenSuccess() {
        when(organizationRepositoryPort.existsByCode("ACME")).thenReturn(false);
        when(organizationMapper.toEntity(createRequest)).thenReturn(orgEntity);
        when(securityAuditHelper.getCurrentUsername()).thenReturn("admin");
        when(organizationRepositoryPort.save(any(OrganizationEntity.class))).thenReturn(orgEntity);
        when(organizationMapper.toResponse(orgEntity)).thenReturn(orgResponse);

        OrganizationResponse response = organizationService.createOrganization(createRequest);

        assertNotNull(response);
        assertEquals("ACME", response.getCode());
        verify(organizationRepositoryPort, times(1)).save(any(OrganizationEntity.class));
    }

    @Test
    void whenCreateOrganization_withDuplicateCode_thenThrowValidationException() {
        when(organizationRepositoryPort.existsByCode("ACME")).thenReturn(true);

        assertThrows(ValidationException.class, () -> organizationService.createOrganization(createRequest));
        verify(organizationRepositoryPort, never()).save(any());
    }

    @Test
    void whenUpdateOrganization_withValidData_thenSuccess() {
        when(organizationRepositoryPort.findById(orgId)).thenReturn(Optional.of(orgEntity));
        when(securityAuditHelper.getCurrentUsername()).thenReturn("admin");
        when(organizationRepositoryPort.save(any(OrganizationEntity.class))).thenReturn(orgEntity);
        when(organizationMapper.toResponse(orgEntity)).thenReturn(orgResponse);

        OrganizationResponse response = organizationService.updateOrganization(updateRequest);

        assertNotNull(response);
        verify(organizationRepositoryPort, times(1)).save(any(OrganizationEntity.class));
    }

    @Test
    void whenDeleteOrganization_withExistingId_thenSuccess() {
        when(organizationRepositoryPort.findById(orgId)).thenReturn(Optional.of(orgEntity));
        when(securityAuditHelper.getCurrentUsername()).thenReturn("admin");

        organizationService.deleteOrganization(orgId);

        verify(organizationRepositoryPort, times(1)).deleteById(orgId);
    }

    @Test
    void whenGetOrganizationAuditLogs_withExistingId_thenReturnLogs() {
        when(organizationRepositoryPort.findById(orgId)).thenReturn(Optional.of(orgEntity));
        AuditLogEntity logEntity = AuditLogEntity.builder()
                .logId(UUID.randomUUID())
                .action("ORGANIZATION_CREATED")
                .entityId(orgId)
                .details(List.of())
                .build();
        when(auditLogRepositoryPort.findByEntityTypeAndEntityId("ORGANIZATION", orgId)).thenReturn(List.of(logEntity));

        List<OrganizationAuditResponse> logs = organizationService.getOrganizationAuditLogs(orgId);

        assertNotNull(logs);
        assertEquals(1, logs.size());
        assertEquals("ORGANIZATION_CREATED", logs.get(0).getAction());
        verify(auditLogRepositoryPort, times(1)).findByEntityTypeAndEntityId("ORGANIZATION", orgId);
    }
}
