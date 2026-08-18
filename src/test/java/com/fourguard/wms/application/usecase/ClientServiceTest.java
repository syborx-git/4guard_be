package com.fourguard.wms.application.usecase;

import com.fourguard.wms.application.dto.common.ClientContactDto;
import com.fourguard.wms.application.dto.common.PhysicalDestinationDto;
import com.fourguard.wms.application.dto.request.CreateClientRequest;
import com.fourguard.wms.application.dto.request.UpdateClientRequest;
import com.fourguard.wms.application.dto.response.ClientResponse;
import com.fourguard.wms.application.dto.response.audit.ClientAuditResponse;
import com.fourguard.wms.application.mapper.ClientMapper;
import com.fourguard.wms.domain.exception.EntityNotFoundException;
import com.fourguard.wms.domain.exception.ValidationException;
import com.fourguard.wms.domain.ports.out.AuditLogRepositoryPort;
import com.fourguard.wms.domain.ports.out.ClientDestinationRepositoryPort;
import com.fourguard.wms.domain.ports.out.ClientRepositoryPort;
import com.fourguard.wms.domain.ports.out.OrganizationRepositoryPort;
import com.fourguard.wms.domain.ports.out.UserRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.AuditLogEntity;
import com.fourguard.wms.infrastructure.persistence.entity.ClientContactEntity;
import com.fourguard.wms.infrastructure.persistence.entity.ClientDestinationEntity;
import com.fourguard.wms.infrastructure.persistence.entity.ClientEntity;
import com.fourguard.wms.infrastructure.persistence.entity.OrganizationEntity;
import com.fourguard.wms.shared.audit.AuditService;
import com.fourguard.wms.shared.audit.SecurityAuditHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClientService — Gestión de Clientes, Contactos y Destinos Físicos")
class ClientServiceTest {

    @Mock private ClientRepositoryPort            clientRepositoryPort;
    @Mock private ClientDestinationRepositoryPort destinationRepositoryPort;
    @Mock private OrganizationRepositoryPort      organizationRepositoryPort;
    @Mock private UserRepositoryPort              userRepositoryPort;
    @Mock private AuditLogRepositoryPort          auditLogRepositoryPort;
    @Mock private ClientMapper                    clientMapper;
    @Mock private SecurityAuditHelper             securityAuditHelper;
    @Mock private AuditService                    auditService;

    @InjectMocks
    private ClientService clientService;

    private UUID orgId;
    private UUID clientId;
    private UUID destId;
    private OrganizationEntity orgEntity;
    private ClientEntity clientEntity;
    private ClientEntity clientEntityWithDestinations;
    private ClientResponse clientResponse;
    private CreateClientRequest createRequest;
    private UpdateClientRequest updateRequest;

    @BeforeEach
    void setUp() {
        orgId    = UUID.randomUUID();
        clientId = UUID.randomUUID();
        destId   = UUID.randomUUID();

        orgEntity = OrganizationEntity.builder()
                .id(orgId)
                .name("4GUARD LOGISTICS CORP")
                .code("4GUARD")
                .build();

        clientEntity = ClientEntity.builder()
                .id(clientId)
                .organization(orgEntity)
                .name("NESTLE MEXICO S.A. DE C.V.")
                .externalId("NME850101K99")
                .taxId("NME850101K99")
                .address("Av. Ejército Nacional 453, Granada, CDMX")
                .phone("55 5268 2000")
                .email("logistica@nestle.com.mx")
                .status("ACTIVE")
                .contacts(new ArrayList<>())
                .destinations(new ArrayList<>())
                .build();

        clientResponse = ClientResponse.builder()
                .id(clientId)
                .organizationId(orgId)
                .organizationName("4GUARD LOGISTICS CORP")
                .name("NESTLE MEXICO S.A. DE C.V.")
                .externalId("NME850101K99")
                .address("Av. Ejército Nacional 453, Granada, CDMX")
                .phone("55 5268 2000")
                .status("ACTIVE")
                .contacts(new ArrayList<>())
                .destinations(new ArrayList<>())
                .build();

        createRequest = CreateClientRequest.builder()
                .organizationId(orgId)
                .name("NESTLE MEXICO S.A. DE C.V.")
                .externalId("NME850101K99")
                .taxId("NME850101K99")
                .address("Av. Ejército Nacional 453, Granada, CDMX")
                .phone("55 5268 2000")
                .email("logistica@nestle.com.mx")
                .build();

        updateRequest = UpdateClientRequest.builder()
                .id(clientId)
                .organizationId(orgId)
                .name("NESTLE MEXICO S.A. DE C.V. ACTUALIZADO")
                .externalId("NME850101K99")
                .taxId("NME850101K99")
                .address("Av. Nueva Dirección 100, CDMX")
                .phone("55 9999 0000")
                .status("ACTIVE")
                .build();
    }

    // ── CRUD Principal ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Crear cliente con datos válidos → éxito con contactos y destinos")
    void whenCreateClient_withValidData_thenSuccess() {
        when(organizationRepositoryPort.findById(orgId)).thenReturn(Optional.of(orgEntity));
        when(clientRepositoryPort.existsByOrganizationIdAndExternalId(orgId, "NME850101K99")).thenReturn(false);
        when(clientRepositoryPort.existsByOrganizationIdAndTaxId(orgId, "NME850101K99")).thenReturn(false);
        when(clientMapper.toEntity(createRequest)).thenReturn(clientEntity);
        when(securityAuditHelper.getCurrentUsername()).thenReturn("admin.4guard");
        when(clientRepositoryPort.save(any(ClientEntity.class))).thenReturn(clientEntity);
        when(clientMapper.toResponse(clientEntity)).thenReturn(clientResponse);

        ClientResponse response = clientService.createClient(createRequest);

        assertNotNull(response);
        assertEquals("NESTLE MEXICO S.A. DE C.V.", response.getName());
        assertEquals("ACTIVE", response.getStatus());
        verify(clientRepositoryPort, times(1)).save(any(ClientEntity.class));
    }

    @Test
    @DisplayName("Crear cliente con External ID duplicado → ValidationException")
    void whenCreateClient_withDuplicateExternalId_thenThrowValidationException() {
        when(organizationRepositoryPort.findById(orgId)).thenReturn(Optional.of(orgEntity));
        when(clientRepositoryPort.existsByOrganizationIdAndExternalId(orgId, "NME850101K99")).thenReturn(true);

        assertThrows(ValidationException.class, () -> clientService.createClient(createRequest));
        verify(clientRepositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("Crear cliente con RFC genérico → éxito aunque exista otro con mismo RFC")
    void whenCreateClient_withGenericTaxId_thenSuccessEvenIfDuplicate() {
        CreateClientRequest genericRequest = CreateClientRequest.builder()
                .organizationId(orgId)
                .name("Público en General")
                .taxId("XAXX010101000")
                .address("Calle Principal 1")
                .phone("55 0000 0000")
                .build();

        when(organizationRepositoryPort.findById(orgId)).thenReturn(Optional.of(orgEntity));
        // externalId es null → la validación retorna temprano; stub en modo lenient para evitar UnnecessaryStubbing
        lenient().when(clientRepositoryPort.existsByOrganizationIdAndExternalId(any(), any())).thenReturn(false);
        when(clientMapper.toEntity(genericRequest)).thenReturn(clientEntity);
        when(securityAuditHelper.getCurrentUsername()).thenReturn("admin.4guard");
        when(clientRepositoryPort.save(any(ClientEntity.class))).thenReturn(clientEntity);
        when(clientMapper.toResponse(clientEntity)).thenReturn(clientResponse);

        ClientResponse response = clientService.createClient(genericRequest);

        assertNotNull(response);
        // RFC genérico nunca debe validarse por unicidad
        verify(clientRepositoryPort, never()).existsByOrganizationIdAndTaxId(any(), eq("XAXX010101000"));
    }


    @Test
    @DisplayName("Actualizar cliente con datos válidos → éxito")
    void whenUpdateClient_withValidData_thenSuccess() {
        when(clientRepositoryPort.findById(clientId)).thenReturn(Optional.of(clientEntity));
        when(organizationRepositoryPort.findById(orgId)).thenReturn(Optional.of(orgEntity));
        when(clientRepositoryPort.existsByOrganizationIdAndExternalIdAndIdNot(orgId, "NME850101K99", clientId)).thenReturn(false);
        when(clientRepositoryPort.existsByOrganizationIdAndTaxIdAndIdNot(orgId, "NME850101K99", clientId)).thenReturn(false);
        when(securityAuditHelper.getCurrentUsername()).thenReturn("admin.4guard");
        when(clientRepositoryPort.save(any(ClientEntity.class))).thenReturn(clientEntity);
        when(clientMapper.toResponse(clientEntity)).thenReturn(clientResponse);

        ClientResponse response = clientService.updateClient(updateRequest);

        assertNotNull(response);
        verify(clientRepositoryPort, times(1)).save(any(ClientEntity.class));
    }

    @Test
    @DisplayName("Eliminar cliente existente → éxito con auditoría")
    void whenDeleteClient_withExistingId_thenSuccess() {
        when(clientRepositoryPort.findById(clientId)).thenReturn(Optional.of(clientEntity));
        when(securityAuditHelper.getCurrentUsername()).thenReturn("admin.4guard");

        clientService.deleteClient(clientId);

        verify(clientRepositoryPort, times(1)).deleteById(clientId);
    }

    @Test
    @DisplayName("Obtener cliente inexistente → EntityNotFoundException")
    void whenGetClientById_withInvalidId_thenThrowEntityNotFoundException() {
        UUID invalidId = UUID.randomUUID();
        when(clientRepositoryPort.findById(invalidId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> clientService.getClientById(invalidId));
    }

    @Test
    @DisplayName("Toggle de estado ACTIVE → INACTIVE")
    void whenToggleStatus_fromActive_thenBecomesInactive() {
        when(clientRepositoryPort.findById(clientId)).thenReturn(Optional.of(clientEntity));
        when(securityAuditHelper.getCurrentUsername()).thenReturn("admin.4guard");
        when(clientRepositoryPort.save(any(ClientEntity.class))).thenReturn(clientEntity);
        ClientResponse inactiveResponse = ClientResponse.builder()
                .id(clientId).status("INACTIVE").contacts(new ArrayList<>()).destinations(new ArrayList<>()).build();
        when(clientMapper.toResponse(any())).thenReturn(inactiveResponse);

        clientService.toggleClientStatus(clientId);

        verify(clientRepositoryPort).save(argThat(c -> "INACTIVE".equals(c.getStatus())));
    }

    // ── Auditoría ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Obtener logs de auditoría del cliente → lista correcta")
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
    }

    // ── Gestión Granular de Destinos ──────────────────────────────────────────

    @Test
    @DisplayName("Agregar destino físico con código único → éxito")
    void whenAddDestination_withUniqueCode_thenSuccess() {
        PhysicalDestinationDto dto = PhysicalDestinationDto.builder()
                .destinationCode("DEST-TOL-01")
                .plantName("Planta Toluca (Café y Cacao)")
                .fullAddress("Km 62.5 Carretera México-Toluca, Zona Industrial Toluca")
                .contactPerson("Ing. Fernando Ruiz")
                .phone("722 279 1000")
                .status("ACTIVO")
                .build();

        ClientDestinationEntity destEntity = ClientDestinationEntity.builder()
                .id(destId)
                .client(clientEntity)
                .destinationCode("DEST-TOL-01")
                .plantName("Planta Toluca (Café y Cacao)")
                .fullAddress("Km 62.5 Carretera México-Toluca, Zona Industrial Toluca")
                .contactPerson("Ing. Fernando Ruiz")
                .phone("722 279 1000")
                .status("ACTIVO")
                .build();

        PhysicalDestinationDto savedDto = PhysicalDestinationDto.builder()
                .id(destId)
                .destinationCode("DEST-TOL-01")
                .plantName("Planta Toluca (Café y Cacao)")
                .fullAddress("Km 62.5 Carretera México-Toluca, Zona Industrial Toluca")
                .contactPerson("Ing. Fernando Ruiz")
                .phone("722 279 1000")
                .status("ACTIVO")
                .build();

        when(clientRepositoryPort.findById(clientId)).thenReturn(Optional.of(clientEntity));
        when(destinationRepositoryPort.existsByClientIdAndDestinationCode(clientId, "DEST-TOL-01")).thenReturn(false);
        when(clientMapper.toDestinationEntity(dto)).thenReturn(destEntity);
        when(destinationRepositoryPort.save(any())).thenReturn(destEntity);
        when(clientMapper.toDestinationDto(destEntity)).thenReturn(savedDto);
        when(securityAuditHelper.getCurrentUsername()).thenReturn("admin.4guard");

        PhysicalDestinationDto result = clientService.addDestination(clientId, dto);

        assertNotNull(result);
        assertEquals("DEST-TOL-01", result.getDestinationCode());
        verify(destinationRepositoryPort).save(any());
    }

    @Test
    @DisplayName("Agregar destino con código duplicado → ValidationException")
    void whenAddDestination_withDuplicateCode_thenThrowValidationException() {
        PhysicalDestinationDto dto = PhysicalDestinationDto.builder()
                .destinationCode("DEST-TOL-01")
                .plantName("Planta Toluca")
                .fullAddress("Km 62.5 Carretera México-Toluca")
                .contactPerson("Ing. Fernando Ruiz")
                .phone("722 279 1000")
                .build();

        when(clientRepositoryPort.findById(clientId)).thenReturn(Optional.of(clientEntity));
        when(destinationRepositoryPort.existsByClientIdAndDestinationCode(clientId, "DEST-TOL-01")).thenReturn(true);

        assertThrows(ValidationException.class, () -> clientService.addDestination(clientId, dto));
        verify(destinationRepositoryPort, never()).save(any());
    }
}
