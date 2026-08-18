package com.fourguard.wms.application.usecase;

import com.fourguard.wms.application.dto.request.CreateForkliftOperatorRequest;
import com.fourguard.wms.application.dto.request.UpdateForkliftOperatorRequest;
import com.fourguard.wms.application.dto.request.UpdateForkliftOperatorStatusRequest;
import com.fourguard.wms.application.dto.response.ForkliftOperatorResponse;
import com.fourguard.wms.application.mapper.ForkliftOperatorMapper;
import com.fourguard.wms.domain.enums.ForkliftOperatorStatus;
import com.fourguard.wms.domain.enums.LicenseStatus;
import com.fourguard.wms.domain.exception.EntityNotFoundException;
import com.fourguard.wms.domain.exception.ValidationException;
import com.fourguard.wms.domain.ports.out.AuditLogRepositoryPort;
import com.fourguard.wms.domain.ports.out.ForkliftOperatorRepositoryPort;
import com.fourguard.wms.domain.ports.out.OrganizationRepositoryPort;
import com.fourguard.wms.domain.ports.out.UserRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.ForkliftOperatorEntity;
import com.fourguard.wms.infrastructure.persistence.entity.OrganizationEntity;
import com.fourguard.wms.infrastructure.persistence.repository.BranchJpaRepository;
import com.fourguard.wms.infrastructure.persistence.repository.ShiftJpaRepository;
import com.fourguard.wms.shared.audit.AuditService;
import com.fourguard.wms.shared.audit.SecurityAuditHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ForkliftOperatorService — Gestión de Montacarguistas (HU-142)")
class ForkliftOperatorServiceTest {

    @Mock private ForkliftOperatorRepositoryPort operatorRepositoryPort;
    @Mock private OrganizationRepositoryPort     organizationRepositoryPort;
    @Mock private UserRepositoryPort             userRepositoryPort;
    @Mock private AuditLogRepositoryPort         auditLogRepositoryPort;
    @Mock private ShiftJpaRepository             shiftJpaRepository;
    @Mock private BranchJpaRepository            branchJpaRepository;
    @Mock private ForkliftOperatorMapper         mapper;
    @Mock private SecurityAuditHelper            securityAuditHelper;
    @Mock private AuditService                   auditService;

    @InjectMocks
    private ForkliftOperatorService service;

    // ─── Fixtures ────────────────────────────────────────────────────────────────

    private static final UUID ORG_ID  = UUID.fromString("a53f0907-9fa5-4bdf-87db-2eb5e7683935");
    private static final UUID OP_ID   = UUID.randomUUID();
    private static final String USER  = "test_user";

    private OrganizationEntity organization;
    private ForkliftOperatorEntity operatorEntity;

    @BeforeEach
    void setUp() {
        organization = OrganizationEntity.builder()
                .id(ORG_ID)
                .name("4GUARD LOGISTICS CORP")
                .build();

        operatorEntity = ForkliftOperatorEntity.builder()
                .id(OP_ID)
                .organization(organization)
                .code("MC-001")
                .firstName("Juan")
                .lastNamePaternal("Pérez")
                .lastNameMaternal("García")
                .fullName("Juan Pérez García")
                .licenseNumberDc3("LIC-MC-001")
                .licenseExpirationDate(LocalDate.now().plusMonths(6))
                .licenseStatus(LicenseStatus.VIGENTE)
                .status(ForkliftOperatorStatus.ACTIVO)
                .build();
    }

    // ─── CREATE ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createOperator — éxito: registra nuevo montacarguista con código MC-001")
    void createOperator_success() {
        CreateForkliftOperatorRequest request = CreateForkliftOperatorRequest.builder()
                .organizationId(ORG_ID)
                .firstName("Juan")
                .lastNamePaternal("Pérez")
                .lastNameMaternal("García")
                .licenseNumberDc3("LIC-MC-001")
                .licenseExpirationDate(LocalDate.now().plusMonths(6))
                .build();

        ForkliftOperatorResponse expectedResponse = ForkliftOperatorResponse.builder()
                .id(OP_ID)
                .code("MC-001")
                .fullName("Juan Pérez García")
                .licenseStatus(LicenseStatus.VIGENTE.name())
                .build();

        when(organizationRepositoryPort.findById(ORG_ID)).thenReturn(Optional.of(organization));
        when(operatorRepositoryPort.existsByOrganizationIdAndLicenseNumberDc3(ORG_ID, "LIC-MC-001")).thenReturn(false);
        when(operatorRepositoryPort.countByOrganizationId(ORG_ID)).thenReturn(0);
        when(mapper.toEntity(request)).thenReturn(operatorEntity);
        when(operatorRepositoryPort.save(any())).thenReturn(operatorEntity);
        when(mapper.toResponse(operatorEntity)).thenReturn(expectedResponse);
        when(securityAuditHelper.getCurrentUsername()).thenReturn(USER);
        when(userRepositoryPort.findByUsername(USER)).thenReturn(Optional.empty());

        ForkliftOperatorResponse result = service.createOperator(request);

        assertNotNull(result);
        assertEquals("MC-001", result.getCode());
        assertEquals("Juan Pérez García", result.getFullName());
        verify(operatorRepositoryPort).save(any());
    }

    @Test
    @DisplayName("createOperator — falla: organización no encontrada")
    void createOperator_organizationNotFound() {
        CreateForkliftOperatorRequest request = CreateForkliftOperatorRequest.builder()
                .organizationId(ORG_ID)
                .firstName("Juan").lastNamePaternal("Pérez").lastNameMaternal("García")
                .licenseNumberDc3("LIC-MC-002")
                .licenseExpirationDate(LocalDate.now().plusMonths(6))
                .build();

        when(organizationRepositoryPort.findById(ORG_ID)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.createOperator(request));
        verify(operatorRepositoryPort, never()).save(any());
    }

    @Test
    @DisplayName("createOperator — falla: licencia DC-3 duplicada en la misma organización")
    void createOperator_dc3LicenseDuplicate() {
        CreateForkliftOperatorRequest request = CreateForkliftOperatorRequest.builder()
                .organizationId(ORG_ID)
                .firstName("Juan").lastNamePaternal("Pérez").lastNameMaternal("García")
                .licenseNumberDc3("LIC-MC-001")
                .licenseExpirationDate(LocalDate.now().plusMonths(6))
                .build();

        when(organizationRepositoryPort.findById(ORG_ID)).thenReturn(Optional.of(organization));
        when(operatorRepositoryPort.existsByOrganizationIdAndLicenseNumberDc3(ORG_ID, "LIC-MC-001")).thenReturn(true);

        ValidationException ex = assertThrows(ValidationException.class, () -> service.createOperator(request));
        assertTrue(ex.getMessage().contains("LIC-MC-001"));
        verify(operatorRepositoryPort, never()).save(any());
    }

    // ─── UPDATE ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateOperator — éxito: actualiza datos del montacarguista")
    void updateOperator_success() {
        UpdateForkliftOperatorRequest request = UpdateForkliftOperatorRequest.builder()
                .id(OP_ID)
                .organizationId(ORG_ID)
                .firstName("Juan Manuel")
                .lastNamePaternal("Pérez")
                .lastNameMaternal("García")
                .licenseNumberDc3("LIC-MC-001")
                .licenseExpirationDate(LocalDate.now().plusMonths(12))
                .version(1L)
                .build();

        ForkliftOperatorResponse expectedResponse = ForkliftOperatorResponse.builder()
                .id(OP_ID)
                .fullName("Juan Manuel Pérez García")
                .build();

        when(operatorRepositoryPort.findActiveById(OP_ID)).thenReturn(Optional.of(operatorEntity));
        when(operatorRepositoryPort.save(any())).thenReturn(operatorEntity);
        when(mapper.toResponse(operatorEntity)).thenReturn(expectedResponse);
        when(securityAuditHelper.getCurrentUsername()).thenReturn(USER);
        when(userRepositoryPort.findByUsername(USER)).thenReturn(Optional.empty());

        ForkliftOperatorResponse result = service.updateOperator(request);

        assertNotNull(result);
        verify(operatorRepositoryPort).save(any());
    }

    @Test
    @DisplayName("updateOperator — falla: montacarguista no encontrado")
    void updateOperator_notFound() {
        UpdateForkliftOperatorRequest request = UpdateForkliftOperatorRequest.builder()
                .id(OP_ID)
                .organizationId(ORG_ID)
                .firstName("Juan").lastNamePaternal("Pérez").lastNameMaternal("García")
                .licenseNumberDc3("LIC-MC-001")
                .licenseExpirationDate(LocalDate.now().plusMonths(6))
                .build();

        when(operatorRepositoryPort.findActiveById(OP_ID)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.updateOperator(request));
    }

    // ─── GET ─────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getOperatorById — éxito: retorna montacarguista existente")
    void getOperatorById_success() {
        ForkliftOperatorResponse expectedResponse = ForkliftOperatorResponse.builder()
                .id(OP_ID).code("MC-001").build();

        when(operatorRepositoryPort.findActiveById(OP_ID)).thenReturn(Optional.of(operatorEntity));
        when(mapper.toResponse(operatorEntity)).thenReturn(expectedResponse);

        ForkliftOperatorResponse result = service.getOperatorById(OP_ID);

        assertNotNull(result);
        assertEquals(OP_ID, result.getId());
    }

    @Test
    @DisplayName("getOperatorById — falla: ID no existe")
    void getOperatorById_notFound() {
        when(operatorRepositoryPort.findActiveById(OP_ID)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> service.getOperatorById(OP_ID));
    }

    // ─── STATUS CHANGE ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateOperatorStatus — éxito: cambia de ACTIVO a INACTIVO")
    void updateOperatorStatus_toInactivo() {
        UpdateForkliftOperatorStatusRequest request = UpdateForkliftOperatorStatusRequest.builder()
                .status("INACTIVO")
                .reason("Incapacidad médica")
                .build();

        ForkliftOperatorResponse expectedResponse = ForkliftOperatorResponse.builder()
                .id(OP_ID).status("INACTIVO").build();

        when(operatorRepositoryPort.findActiveById(OP_ID)).thenReturn(Optional.of(operatorEntity));
        when(operatorRepositoryPort.save(any())).thenReturn(operatorEntity);
        when(mapper.toResponse(operatorEntity)).thenReturn(expectedResponse);
        when(securityAuditHelper.getCurrentUsername()).thenReturn(USER);
        when(userRepositoryPort.findByUsername(USER)).thenReturn(Optional.empty());

        ForkliftOperatorResponse result = service.updateOperatorStatus(OP_ID, request);

        assertNotNull(result);
        assertEquals("INACTIVO", result.getStatus());
    }

    @Test
    @DisplayName("updateOperatorStatus — falla: estatus inválido")
    void updateOperatorStatus_invalidStatus() {
        UpdateForkliftOperatorStatusRequest request = UpdateForkliftOperatorStatusRequest.builder()
                .status("SUSPENDIDO")
                .build();

        // NOTE: No need to stub findActiveById — the service validates the status enum first
        // and throws ValidationException before reaching the repository call.
        assertThrows(ValidationException.class, () -> service.updateOperatorStatus(OP_ID, request));
    }


    // ─── DELETE ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteOperator — éxito: realiza soft delete")
    void deleteOperator_success() {
        when(operatorRepositoryPort.findActiveById(OP_ID)).thenReturn(Optional.of(operatorEntity));
        when(securityAuditHelper.getCurrentUsername()).thenReturn(USER);
        when(userRepositoryPort.findByUsername(USER)).thenReturn(Optional.empty());

        service.deleteOperator(OP_ID);

        verify(operatorRepositoryPort).softDeleteById(OP_ID, USER);
    }

    @Test
    @DisplayName("deleteOperator — falla: montacarguista no encontrado")
    void deleteOperator_notFound() {
        when(operatorRepositoryPort.findActiveById(OP_ID)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> service.deleteOperator(OP_ID));
        verify(operatorRepositoryPort, never()).softDeleteById(any(), any());
    }

    // ─── LIST ────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getOperators — retorna lista filtrada por organización")
    void getOperators_byOrganization() {
        ForkliftOperatorResponse operatorResponse = ForkliftOperatorResponse.builder()
                .id(OP_ID).organizationId(ORG_ID).status("ACTIVO").build();

        when(operatorRepositoryPort.findByOrganizationId(ORG_ID)).thenReturn(List.of(operatorEntity));
        when(mapper.toResponse(operatorEntity)).thenReturn(operatorResponse);

        List<ForkliftOperatorResponse> results = service.getOperators(ORG_ID, null, null, null, null);

        assertEquals(1, results.size());
        assertEquals(ORG_ID, results.get(0).getOrganizationId());
    }
}
