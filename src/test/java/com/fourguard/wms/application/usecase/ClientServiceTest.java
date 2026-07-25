package com.fourguard.wms.application.usecase;

import com.fourguard.wms.application.dto.request.CreateClientRequest;
import com.fourguard.wms.application.dto.request.UpdateClientRequest;
import com.fourguard.wms.application.dto.response.ClientResponse;
import com.fourguard.wms.application.dto.response.audit.ClientAuditResponse;
import com.fourguard.wms.application.mapper.ClientMapper;
import com.fourguard.wms.domain.exception.EntityNotFoundException;
import com.fourguard.wms.domain.ports.out.AuditLogRepositoryPort;
import com.fourguard.wms.domain.ports.out.ClientRepositoryPort;
import com.fourguard.wms.domain.ports.out.OrganizationRepositoryPort;
import com.fourguard.wms.domain.ports.out.UserRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.AuditLogEntity;
import com.fourguard.wms.infrastructure.persistence.entity.ClientEntity;
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
class ClientServiceTest {

    @Mock
    private ClientRepositoryPort clientRepositoryPort;
    @Mock
    private OrganizationRepositoryPort organizationRepositoryPort;
    @Mock
    private UserRepositoryPort userRepositoryPort;
    @Mock
    private AuditLogRepositoryPort auditLogRepositoryPort;
    @Mock
    private ClientMapper clientMapper;
    @Mock
    private SecurityAuditHelper securityAuditHelper;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private ClientService clientService;

    private UUID orgId;
    private UUID clientId;
    private OrganizationEntity orgEntity;
    private ClientEntity clientEntity;
    private ClientResponse clientResponse;
    private CreateClientRequest createRequest;
    private UpdateClientRequest updateRequest;

    @BeforeEach
    void setUp() {
        orgId = UUID.randomUUID();
        clientId = UUID.randomUUID();

        orgEntity = OrganizationEntity.builder()
                .id(orgId)
                .name("Acme Corp")
                .code("ACME")
                .build();

        clientEntity = ClientEntity.builder()
                .id(clientId)
                .organization(orgEntity)
                .name("Cliente Logística SA")
                .externalId("CLI-001")
                .status("ACTIVE")
                .build();

        clientResponse = ClientResponse.builder()
                .id(clientId)
                .name("Cliente Logística SA")
                .externalId("CLI-001")
                .status("ACTIVE")
                .build();

        createRequest = CreateClientRequest.builder()
                .organizationId(orgId)
                .name("Cliente Logística SA")
                .externalId("CLI-001")
                .build();

        updateRequest = UpdateClientRequest.builder()
                .id(clientId)
                .organizationId(orgId)
                .name("Cliente Logística SA Actualizado")
                .externalId("CLI-001")
                .status("ACTIVE")
                .build();
    }

    @Test
    void whenCreateClient_withValidData_thenSuccess() {
        when(organizationRepositoryPort.findById(orgId)).thenReturn(Optional.of(orgEntity));
        when(clientMapper.toEntity(createRequest)).thenReturn(clientEntity);
        when(securityAuditHelper.getCurrentUsername()).thenReturn("admin");
        when(clientRepositoryPort.save(any(ClientEntity.class))).thenReturn(clientEntity);
        when(clientMapper.toResponse(clientEntity)).thenReturn(clientResponse);

        ClientResponse response = clientService.createClient(createRequest);

        assertNotNull(response);
        assertEquals("Cliente Logística SA", response.getName());
        verify(clientRepositoryPort, times(1)).save(any(ClientEntity.class));
    }

    @Test
    void whenUpdateClient_withValidData_thenSuccess() {
        when(clientRepositoryPort.findById(clientId)).thenReturn(Optional.of(clientEntity));
        when(organizationRepositoryPort.findById(orgId)).thenReturn(Optional.of(orgEntity));
        when(securityAuditHelper.getCurrentUsername()).thenReturn("admin");
        when(clientRepositoryPort.save(any(ClientEntity.class))).thenReturn(clientEntity);
        when(clientMapper.toResponse(clientEntity)).thenReturn(clientResponse);

        ClientResponse response = clientService.updateClient(updateRequest);

        assertNotNull(response);
        verify(clientRepositoryPort, times(1)).save(any(ClientEntity.class));
    }

    @Test
    void whenDeleteClient_withExistingId_thenSuccess() {
        when(clientRepositoryPort.findById(clientId)).thenReturn(Optional.of(clientEntity));
        when(securityAuditHelper.getCurrentUsername()).thenReturn("admin");

        clientService.deleteClient(clientId);

        verify(clientRepositoryPort, times(1)).deleteById(clientId);
    }

    @Test
    void whenGetClientAuditLogs_withExistingId_thenReturnLogs() {
        when(clientRepositoryPort.findById(clientId)).thenReturn(Optional.of(clientEntity));
        AuditLogEntity logEntity = AuditLogEntity.builder()
                .logId(UUID.randomUUID())
                .action("CLIENT_CREATED")
                .entityId(clientId)
                .details(List.of())
                .build();
        when(auditLogRepositoryPort.findByEntityTypeAndEntityId("CLIENT", clientId)).thenReturn(List.of(logEntity));

        List<ClientAuditResponse> logs = clientService.getClientAuditLogs(clientId);

        assertNotNull(logs);
        assertEquals(1, logs.size());
        assertEquals("CLIENT_CREATED", logs.get(0).getAction());
        verify(auditLogRepositoryPort, times(1)).findByEntityTypeAndEntityId("CLIENT", clientId);
    }
}
