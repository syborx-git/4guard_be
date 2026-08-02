package com.fourguard.wms.application.usecase;

import com.fourguard.wms.application.dto.request.*;
import com.fourguard.wms.application.dto.response.*;
import com.fourguard.wms.application.mapper.WmsLicenseMapper;
import com.fourguard.wms.domain.enums.LicenseAdminStatus;
import com.fourguard.wms.domain.enums.LicenseHistoryAction;
import com.fourguard.wms.domain.enums.LicensePlan;
import com.fourguard.wms.domain.exception.EntityNotFoundException;
import com.fourguard.wms.domain.exception.ValidationException;
import com.fourguard.wms.domain.model.LicenseUsage;
import com.fourguard.wms.domain.model.WmsLicense;
import com.fourguard.wms.domain.model.WmsLicenseHistory;
import com.fourguard.wms.domain.ports.out.UserRepositoryPort;
import com.fourguard.wms.domain.ports.out.WmsLicenseHistoryRepositoryPort;
import com.fourguard.wms.domain.ports.out.WmsLicenseRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.OrganizationEntity;
import com.fourguard.wms.infrastructure.persistence.repository.OrganizationJpaRepository;
import com.fourguard.wms.shared.audit.AuditService;
import com.fourguard.wms.shared.audit.SecurityAuditHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WmsLicenseServiceTest {

    @Mock
    private WmsLicenseRepositoryPort wmsLicenseRepositoryPort;
    @Mock
    private WmsLicenseHistoryRepositoryPort wmsLicenseHistoryRepositoryPort;
    @Mock
    private OrganizationJpaRepository organizationJpaRepository;
    @Mock
    private UserRepositoryPort userRepositoryPort;
    @Spy
    private WmsLicenseMapper wmsLicenseMapper = new WmsLicenseMapper();
    @Mock
    private SecurityAuditHelper securityAuditHelper;
    @Mock
    private AuditService auditService;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private WmsLicenseService wmsLicenseService;

    private UUID orgId;
    private UUID licenseId;
    private OrganizationEntity orgEntity;
    private WmsLicense licenseDomain;
    private CreateLicenseRequest createRequest;

    @BeforeEach
    void setUp() {
        orgId = UUID.randomUUID();
        licenseId = UUID.randomUUID();

        orgEntity = OrganizationEntity.builder()
                .id(orgId)
                .name("4GUARD LOGISTICS CORP")
                .code("4GUARD")
                .build();

        licenseDomain = WmsLicense.builder()
                .id(licenseId)
                .organizationId(orgId)
                .organizationName("4GUARD LOGISTICS CORP")
                .licenseName("Licencia Enterprise Demo")
                .licenseKeyHash("$2a$12$hashedkey")
                .maskedLicenseKey("4GD-ENT-••••-••••-9X21")
                .plan(LicensePlan.ENTERPRISE)
                .validFrom(OffsetDateTime.now().minusDays(10))
                .validUntil(OffsetDateTime.now().plusDays(350))
                .adminStatus(LicenseAdminStatus.ACTIVE)
                .maxUsers(50)
                .maxConcurrentUsers(25)
                .maxWarehouses(5)
                .maxHandheldDevices(20)
                .maxIntegrations(10)
                .enabledModules(List.of("WMS_CORE", "INVENTORY"))
                .updatedBy("admin")
                .build();

        createRequest = CreateLicenseRequest.builder()
                .organizationId(orgId)
                .licenseName("Licencia Enterprise Demo")
                .plan(LicensePlan.ENTERPRISE)
                .validFrom(OffsetDateTime.now())
                .validUntil(OffsetDateTime.now().plusYears(1))
                .maxUsers(50)
                .enabledModules(List.of("WMS_CORE", "INVENTORY"))
                .build();
    }

    @Test
    void whenCreateLicense_withValidRequest_thenSuccess() {
        when(organizationJpaRepository.findById(orgId)).thenReturn(Optional.of(orgEntity));
        when(securityAuditHelper.getCurrentUsername()).thenReturn("admin");
        when(passwordEncoder.encode(any())).thenReturn("hashed_key");
        when(wmsLicenseRepositoryPort.save(any(WmsLicense.class))).thenReturn(licenseDomain);

        WmsLicenseResponse response = wmsLicenseService.createLicense(createRequest);

        assertNotNull(response);
        assertEquals(licenseId, response.getId());
        assertEquals("Licencia Enterprise Demo", response.getLicenseName());
        verify(wmsLicenseRepositoryPort, times(1)).save(any(WmsLicense.class));
        verify(wmsLicenseHistoryRepositoryPort, times(1)).save(any(WmsLicenseHistory.class));
    }

    @Test
    void whenCreateLicense_withInvalidDates_thenThrowValidationException() {
        createRequest.setValidFrom(OffsetDateTime.now().plusDays(10));
        createRequest.setValidUntil(OffsetDateTime.now().plusDays(1));

        when(organizationJpaRepository.findById(orgId)).thenReturn(Optional.of(orgEntity));
        when(securityAuditHelper.getCurrentUsername()).thenReturn("admin");

        assertThrows(ValidationException.class, () -> wmsLicenseService.createLicense(createRequest));
        verify(wmsLicenseRepositoryPort, never()).save(any());
    }

    @Test
    void whenUpdateLicense_withValidData_thenSuccess() {
        when(wmsLicenseRepositoryPort.findById(licenseId)).thenReturn(Optional.of(licenseDomain));
        when(securityAuditHelper.getCurrentUsername()).thenReturn("admin");
        when(wmsLicenseRepositoryPort.save(any(WmsLicense.class))).thenReturn(licenseDomain);

        UpdateLicenseRequest updateRequest = UpdateLicenseRequest.builder()
                .licenseName("Licencia Enterprise Modificada")
                .maxUsers(100)
                .build();

        WmsLicenseResponse response = wmsLicenseService.updateLicense(licenseId, updateRequest);

        assertNotNull(response);
        verify(wmsLicenseRepositoryPort, times(1)).save(any(WmsLicense.class));
        verify(wmsLicenseHistoryRepositoryPort, times(1)).save(any(WmsLicenseHistory.class));
    }

    @Test
    void whenRenewLicense_withValidRequest_thenSuccess() {
        when(wmsLicenseRepositoryPort.findById(licenseId)).thenReturn(Optional.of(licenseDomain));
        when(securityAuditHelper.getCurrentUsername()).thenReturn("admin");
        when(wmsLicenseRepositoryPort.save(any(WmsLicense.class))).thenReturn(licenseDomain);

        RenewLicenseRequest renewRequest = RenewLicenseRequest.builder()
                .newValidUntil(OffsetDateTime.now().plusYears(2))
                .reason("Renovación anticipada por contrato de 2 años")
                .build();

        WmsLicenseResponse response = wmsLicenseService.renewLicense(licenseId, renewRequest);

        assertNotNull(response);
        verify(wmsLicenseRepositoryPort, times(1)).save(any(WmsLicense.class));
        verify(wmsLicenseHistoryRepositoryPort, times(1)).save(any(WmsLicenseHistory.class));
    }

    @Test
    void whenSuspendLicense_withValidReason_thenSuccess() {
        when(wmsLicenseRepositoryPort.findById(licenseId)).thenReturn(Optional.of(licenseDomain));
        when(securityAuditHelper.getCurrentUsername()).thenReturn("admin");
        when(wmsLicenseRepositoryPort.save(any(WmsLicense.class))).thenAnswer(i -> i.getArgument(0));

        SuspendLicenseRequest suspendRequest = SuspendLicenseRequest.builder()
                .reason("Falta de pago de cuota anual")
                .build();

        WmsLicenseResponse response = wmsLicenseService.suspendLicense(licenseId, suspendRequest);

        assertNotNull(response);
        assertEquals(LicenseAdminStatus.SUSPENDED, response.getAdminStatus());
        verify(wmsLicenseRepositoryPort, times(1)).save(any(WmsLicense.class));
    }

    @Test
    void whenReactivateLicense_whenSuspended_thenSuccess() {
        licenseDomain.setAdminStatus(LicenseAdminStatus.SUSPENDED);
        when(wmsLicenseRepositoryPort.findById(licenseId)).thenReturn(Optional.of(licenseDomain));
        when(securityAuditHelper.getCurrentUsername()).thenReturn("admin");
        when(wmsLicenseRepositoryPort.save(any(WmsLicense.class))).thenAnswer(i -> i.getArgument(0));

        WmsLicenseResponse response = wmsLicenseService.reactivateLicense(licenseId);

        assertNotNull(response);
        assertEquals(LicenseAdminStatus.ACTIVE, response.getAdminStatus());
        verify(wmsLicenseRepositoryPort, times(1)).save(any(WmsLicense.class));
    }

    @Test
    void whenReactivateLicense_whenActive_thenThrowValidationException() {
        licenseDomain.setAdminStatus(LicenseAdminStatus.ACTIVE);
        when(wmsLicenseRepositoryPort.findById(licenseId)).thenReturn(Optional.of(licenseDomain));

        assertThrows(ValidationException.class, () -> wmsLicenseService.reactivateLicense(licenseId));
        verify(wmsLicenseRepositoryPort, never()).save(any());
    }

    @Test
    void whenRevokeLicense_thenSuccess() {
        when(wmsLicenseRepositoryPort.findById(licenseId)).thenReturn(Optional.of(licenseDomain));
        when(securityAuditHelper.getCurrentUsername()).thenReturn("admin");
        when(wmsLicenseRepositoryPort.save(any(WmsLicense.class))).thenAnswer(i -> i.getArgument(0));

        WmsLicenseResponse response = wmsLicenseService.revokeLicense(licenseId, "Cancelación de contrato");

        assertNotNull(response);
        assertEquals(LicenseAdminStatus.REVOKED, response.getAdminStatus());
    }

    @Test
    void whenRegenerateKey_thenReturnsNewRawKey() {
        when(wmsLicenseRepositoryPort.findById(licenseId)).thenReturn(Optional.of(licenseDomain));
        when(securityAuditHelper.getCurrentUsername()).thenReturn("admin");
        when(passwordEncoder.encode(any())).thenReturn("new_hash");
        when(wmsLicenseRepositoryPort.save(any(WmsLicense.class))).thenReturn(licenseDomain);

        LicenseKeyGeneratedResponse response = wmsLicenseService.regenerateKey(licenseId);

        assertNotNull(response);
        assertNotNull(response.getRawLicenseKey());
        assertTrue(response.getRawLicenseKey().startsWith("4GD-ENT-"));
        assertNotNull(response.getMaskedLicenseKey());
    }

    @Test
    void whenGetLicenseById_thenReturnsDetailWithUsage() {
        when(wmsLicenseRepositoryPort.findById(licenseId)).thenReturn(Optional.of(licenseDomain));

        LicenseUsage usage = LicenseUsage.builder()
                .licenseId(licenseId)
                .currentUsers(3L)
                .concurrentUsersPeak(1L)
                .currentWarehouses(1L)
                .registeredHandheldDevices(0L)
                .activeIntegrations(0L)
                .build();
        when(wmsLicenseRepositoryPort.findUsageByLicenseId(licenseId)).thenReturn(Optional.of(usage));

        LicenseDetailResponse detail = wmsLicenseService.getLicenseById(licenseId);

        assertNotNull(detail);
        assertEquals(licenseId, detail.getLicense().getId());
        assertEquals(3L, detail.getUsage().getCurrentUsers());
    }

    @Test
    void whenGetLicenseHistory_thenReturnsAuditList() {
        when(wmsLicenseRepositoryPort.findById(licenseId)).thenReturn(Optional.of(licenseDomain));

        WmsLicenseHistory history = WmsLicenseHistory.builder()
                .id(UUID.randomUUID())
                .licenseId(licenseId)
                .action(LicenseHistoryAction.CREATED)
                .description("Creación inicial")
                .performedBy("admin")
                .performedAt(OffsetDateTime.now())
                .build();

        when(wmsLicenseHistoryRepositoryPort.findByLicenseIdOrderByPerformedAtDesc(licenseId)).thenReturn(List.of(history));

        List<WmsLicenseHistoryResponse> historyResponses = wmsLicenseService.getLicenseHistory(licenseId);

        assertNotNull(historyResponses);
        assertEquals(1, historyResponses.size());
        assertEquals(LicenseHistoryAction.CREATED, historyResponses.get(0).getAction());
    }
}
