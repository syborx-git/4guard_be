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
import com.fourguard.wms.domain.ports.in.WmsLicenseUseCase;
import com.fourguard.wms.domain.ports.out.WmsLicenseHistoryRepositoryPort;
import com.fourguard.wms.domain.ports.out.WmsLicenseRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.OrganizationEntity;
import com.fourguard.wms.infrastructure.persistence.entity.UserEntity;
import com.fourguard.wms.infrastructure.persistence.repository.OrganizationJpaRepository;
import com.fourguard.wms.domain.ports.out.UserRepositoryPort;
import com.fourguard.wms.shared.audit.AuditService;
import com.fourguard.wms.shared.audit.SecurityAuditHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WmsLicenseService implements WmsLicenseUseCase {

    private static final String KEY_CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final WmsLicenseRepositoryPort wmsLicenseRepositoryPort;
    private final WmsLicenseHistoryRepositoryPort wmsLicenseHistoryRepositoryPort;
    private final OrganizationJpaRepository organizationJpaRepository;
    private final UserRepositoryPort userRepositoryPort;
    private final WmsLicenseMapper wmsLicenseMapper;
    private final SecurityAuditHelper securityAuditHelper;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public WmsLicenseResponse createLicense(CreateLicenseRequest request) {
        log.info("Creating WMS License for organization ID: {}", request.getOrganizationId());

        String currentUser = securityAuditHelper.getCurrentUsername();
        OrganizationEntity org = organizationJpaRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new EntityNotFoundException("Organización no encontrada con ID: " + request.getOrganizationId()));

        if (request.getValidFrom().isAfter(request.getValidUntil())) {
            throw new ValidationException("La fecha de inicio (validFrom) debe ser anterior a la fecha de vencimiento (validUntil).");
        }

        String rawKey = generateRawLicenseKey(request.getPlan());
        String maskedKey = maskLicenseKey(rawKey);
        String keyHash = passwordEncoder.encode(rawKey);

        WmsLicense license = wmsLicenseMapper.toDomainModel(request);
        license.setOrganizationName(org.getName());
        license.setLicenseKeyHash(keyHash);
        license.setMaskedLicenseKey(maskedKey);
        license.setAdminStatus(LicenseAdminStatus.ACTIVE);
        license.setCreatedAt(OffsetDateTime.now());
        license.setUpdatedAt(OffsetDateTime.now());
        license.setUpdatedBy(currentUser);

        WmsLicense saved = wmsLicenseRepositoryPort.save(license);

        // Record history
        recordHistory(saved.getId(), LicenseHistoryAction.CREATED,
                "Emisión inicial de licencia WMS para " + org.getName(),
                null, buildStateJson(saved), currentUser);

        logAuditChange(currentUser, "LICENSE_CREATED", saved.getId(), null, buildStateMap(saved));

        return wmsLicenseMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public WmsLicenseResponse updateLicense(UUID id, UpdateLicenseRequest request) {
        log.info("Updating WMS License ID: {}", id);

        WmsLicense existing = wmsLicenseRepositoryPort.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Licencia WMS no encontrada con ID: " + id));

        String currentUser = securityAuditHelper.getCurrentUsername();
        String previousStateJson = buildStateJson(existing);
        Map<String, Object> previousStateMap = buildStateMap(existing);

        boolean capacityChanged = false;
        boolean modulesChanged = false;

        if (request.getLicenseName() != null) existing.setLicenseName(request.getLicenseName());
        if (request.getPlan() != null) existing.setPlan(request.getPlan());
        if (request.getDescription() != null) existing.setDescription(request.getDescription());
        if (request.getValidFrom() != null) existing.setValidFrom(request.getValidFrom());
        if (request.getValidUntil() != null) existing.setValidUntil(request.getValidUntil());
        if (request.getGracePeriodDays() != null) existing.setGracePeriodDays(request.getGracePeriodDays());
        if (request.getAutoRenewal() != null) existing.setAutoRenewal(request.getAutoRenewal());

        // Capacity limits
        if (request.getMaxUsers() != null && !request.getMaxUsers().equals(existing.getMaxUsers())) {
            existing.setMaxUsers(request.getMaxUsers());
            capacityChanged = true;
        }
        if (request.getMaxConcurrentUsers() != null && !request.getMaxConcurrentUsers().equals(existing.getMaxConcurrentUsers())) {
            existing.setMaxConcurrentUsers(request.getMaxConcurrentUsers());
            capacityChanged = true;
        }
        if (request.getMaxWarehouses() != null && !request.getMaxWarehouses().equals(existing.getMaxWarehouses())) {
            existing.setMaxWarehouses(request.getMaxWarehouses());
            capacityChanged = true;
        }
        if (request.getMaxHandheldDevices() != null && !request.getMaxHandheldDevices().equals(existing.getMaxHandheldDevices())) {
            existing.setMaxHandheldDevices(request.getMaxHandheldDevices());
            capacityChanged = true;
        }
        if (request.getMaxIntegrations() != null && !request.getMaxIntegrations().equals(existing.getMaxIntegrations())) {
            existing.setMaxIntegrations(request.getMaxIntegrations());
            capacityChanged = true;
        }

        // Modules
        if (request.getEnabledModules() != null && !request.getEnabledModules().equals(existing.getEnabledModules())) {
            existing.setEnabledModules(request.getEnabledModules());
            modulesChanged = true;
        }

        if (request.getAdministrativeReason() != null) existing.setAdministrativeReason(request.getAdministrativeReason());
        if (request.getObservations() != null) existing.setObservations(request.getObservations());

        existing.setUpdatedAt(OffsetDateTime.now());
        existing.setUpdatedBy(currentUser);

        WmsLicense saved = wmsLicenseRepositoryPort.save(existing);

        // Record History Action
        LicenseHistoryAction action = LicenseHistoryAction.UPDATED;
        String desc = "Actualización de metadatos de licencia";
        if (capacityChanged && modulesChanged) {
            action = LicenseHistoryAction.CAPACITY_CHANGED;
            desc = "Modificación de capacidades contratadas y módulos habilitados";
        } else if (capacityChanged) {
            action = LicenseHistoryAction.CAPACITY_CHANGED;
            desc = "Modificación de capacidades contratadas";
        } else if (modulesChanged) {
            action = LicenseHistoryAction.MODULES_CHANGED;
            desc = "Modificación de módulos habilitados";
        }

        recordHistory(saved.getId(), action, desc, previousStateJson, buildStateJson(saved), currentUser);
        logAuditChange(currentUser, "LICENSE_UPDATED", saved.getId(), previousStateMap, buildStateMap(saved));

        return wmsLicenseMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public WmsLicenseResponse renewLicense(UUID id, RenewLicenseRequest request) {
        log.info("Renewing WMS License ID: {}", id);

        WmsLicense existing = wmsLicenseRepositoryPort.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Licencia WMS no encontrada con ID: " + id));

        String currentUser = securityAuditHelper.getCurrentUsername();
        String previousStateJson = buildStateJson(existing);

        if (request.getNewValidUntil().isBefore(OffsetDateTime.now())) {
            throw new ValidationException("La nueva fecha de vencimiento debe ser futura.");
        }

        existing.setValidUntil(request.getNewValidUntil());
        if (request.getNewPlan() != null) {
            existing.setPlan(request.getNewPlan());
        }
        if (request.getAutoRenewal() != null) {
            existing.setAutoRenewal(request.getAutoRenewal());
        }
        if (existing.getAdminStatus() == LicenseAdminStatus.SUSPENDED) {
            existing.setAdminStatus(LicenseAdminStatus.ACTIVE);
            existing.setAdministrativeReason("Licencia reactivada automáticamente por renovación contractual.");
        }

        existing.setUpdatedAt(OffsetDateTime.now());
        existing.setUpdatedBy(currentUser);

        WmsLicense saved = wmsLicenseRepositoryPort.save(existing);

        String reason = request.getReason() != null ? request.getReason() : "Renovación contractual exitosa";
        recordHistory(saved.getId(), LicenseHistoryAction.RENEWED, "Renovación de licencia: " + reason,
                previousStateJson, buildStateJson(saved), currentUser);

        return wmsLicenseMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public WmsLicenseResponse suspendLicense(UUID id, SuspendLicenseRequest request) {
        log.info("Suspending WMS License ID: {}", id);

        WmsLicense existing = wmsLicenseRepositoryPort.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Licencia WMS no encontrada con ID: " + id));

        if (existing.getAdminStatus() == LicenseAdminStatus.REVOKED) {
            throw new ValidationException("No se puede suspender una licencia que ya ha sido revocada permanentemente.");
        }

        String currentUser = securityAuditHelper.getCurrentUsername();
        String previousStateJson = buildStateJson(existing);

        existing.setAdminStatus(LicenseAdminStatus.SUSPENDED);
        existing.setAdministrativeReason(request.getReason());
        existing.setUpdatedAt(OffsetDateTime.now());
        existing.setUpdatedBy(currentUser);

        WmsLicense saved = wmsLicenseRepositoryPort.save(existing);

        recordHistory(saved.getId(), LicenseHistoryAction.SUSPENDED,
                "Suspensión administrativa: " + request.getReason(),
                previousStateJson, buildStateJson(saved), currentUser);

        return wmsLicenseMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public WmsLicenseResponse reactivateLicense(UUID id) {
        log.info("Reactivating WMS License ID: {}", id);

        WmsLicense existing = wmsLicenseRepositoryPort.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Licencia WMS no encontrada con ID: " + id));

        if (existing.getAdminStatus() != LicenseAdminStatus.SUSPENDED) {
            throw new ValidationException("Solo se pueden reactivar licencias en estatus SUSPENDED.");
        }

        String currentUser = securityAuditHelper.getCurrentUsername();
        String previousStateJson = buildStateJson(existing);

        existing.setAdminStatus(LicenseAdminStatus.ACTIVE);
        existing.setAdministrativeReason("Reactivada administrativamente");
        existing.setUpdatedAt(OffsetDateTime.now());
        existing.setUpdatedBy(currentUser);

        WmsLicense saved = wmsLicenseRepositoryPort.save(existing);

        recordHistory(saved.getId(), LicenseHistoryAction.REACTIVATED,
                "Reactivación administrativa de licencia",
                previousStateJson, buildStateJson(saved), currentUser);

        return wmsLicenseMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public WmsLicenseResponse revokeLicense(UUID id, String reason) {
        log.info("Revoking WMS License ID: {}", id);

        WmsLicense existing = wmsLicenseRepositoryPort.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Licencia WMS no encontrada con ID: " + id));

        String currentUser = securityAuditHelper.getCurrentUsername();
        String previousStateJson = buildStateJson(existing);

        existing.setAdminStatus(LicenseAdminStatus.REVOKED);
        existing.setAdministrativeReason(reason != null ? reason : "Revocación permanente de licencia");
        existing.setUpdatedAt(OffsetDateTime.now());
        existing.setUpdatedBy(currentUser);

        WmsLicense saved = wmsLicenseRepositoryPort.save(existing);

        recordHistory(saved.getId(), LicenseHistoryAction.REVOKED,
                "Revocación permanente: " + existing.getAdministrativeReason(),
                previousStateJson, buildStateJson(saved), currentUser);

        return wmsLicenseMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public LicenseKeyGeneratedResponse regenerateKey(UUID id) {
        log.info("Regenerating Key for WMS License ID: {}", id);

        WmsLicense existing = wmsLicenseRepositoryPort.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Licencia WMS no encontrada con ID: " + id));

        if (existing.getAdminStatus() == LicenseAdminStatus.REVOKED) {
            throw new ValidationException("No se puede regenerar la clave de una licencia revocada.");
        }

        String currentUser = securityAuditHelper.getCurrentUsername();
        String previousStateJson = buildStateJson(existing);

        String rawKey = generateRawLicenseKey(existing.getPlan());
        String maskedKey = maskLicenseKey(rawKey);
        String keyHash = passwordEncoder.encode(rawKey);

        existing.setLicenseKeyHash(keyHash);
        existing.setMaskedLicenseKey(maskedKey);
        existing.setUpdatedAt(OffsetDateTime.now());
        existing.setUpdatedBy(currentUser);

        WmsLicense saved = wmsLicenseRepositoryPort.save(existing);

        recordHistory(saved.getId(), LicenseHistoryAction.KEY_REGENERATED,
                "Regeneración y re-encriptación de clave secreta de licencia",
                previousStateJson, buildStateJson(saved), currentUser);

        return LicenseKeyGeneratedResponse.builder()
                .licenseId(saved.getId())
                .rawLicenseKey(rawKey)
                .maskedLicenseKey(maskedKey)
                .message("Clave secreta de licencia regenerada con éxito. Guarde esta clave plana en un lugar seguro.")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public LicenseDetailResponse getLicenseById(UUID id) {
        log.debug("Fetching WMS License detail by ID: {}", id);

        WmsLicense license = wmsLicenseRepositoryPort.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Licencia WMS no encontrada con ID: " + id));

        WmsLicenseResponse licenseResponse = wmsLicenseMapper.toResponse(license);
        LicenseUsage usage = wmsLicenseRepositoryPort.findUsageByLicenseId(id)
                .orElse(LicenseUsage.builder()
                        .licenseId(id)
                        .currentUsers(0L)
                        .concurrentUsersPeak(0L)
                        .currentWarehouses(0L)
                        .registeredHandheldDevices(0L)
                        .activeIntegrations(0L)
                        .build());

        LicenseUsageResponse usageResponse = wmsLicenseMapper.toUsageResponse(usage);

        return LicenseDetailResponse.builder()
                .license(licenseResponse)
                .usage(usageResponse)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WmsLicenseResponse> getAllLicenses(UUID organizationId) {
        log.debug("Fetching WMS Licenses for organizationId: {}", organizationId);

        List<WmsLicense> licenses;
        if (organizationId != null) {
            licenses = wmsLicenseRepositoryPort.findByOrganizationId(organizationId);
        } else {
            licenses = wmsLicenseRepositoryPort.findAll();
        }

        return licenses.stream()
                .map(wmsLicenseMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WmsLicenseHistoryResponse> getLicenseHistory(UUID id) {
        log.debug("Fetching history for WMS License ID: {}", id);

        if (wmsLicenseRepositoryPort.findById(id).isEmpty()) {
            throw new EntityNotFoundException("Licencia WMS no encontrada con ID: " + id);
        }

        return wmsLicenseHistoryRepositoryPort.findByLicenseIdOrderByPerformedAtDesc(id).stream()
                .map(wmsLicenseMapper::toHistoryResponse)
                .collect(Collectors.toList());
    }

    // ── Helper Methods ────────────────────────────────────────────────────────

    private String generateRawLicenseKey(LicensePlan plan) {
        String prefix = "4GD-" + (plan != null ? plan.name().substring(0, Math.min(3, plan.name().length())) : "ENT");
        String part1 = randomString(4);
        String part2 = randomString(4);
        String part3 = randomString(4);
        return String.format("%s-%s-%s-%s", prefix, part1, part2, part3);
    }

    private String maskLicenseKey(String rawKey) {
        if (rawKey == null || rawKey.length() < 12) {
            return "4GD-ENT-••••-••••-9X21";
        }
        String[] parts = rawKey.split("-");
        if (parts.length == 4) {
            return String.format("%s-%s-••••-••••-%s", parts[0], parts[1], parts[3]);
        }
        return "4GD-ENT-••••-••••-" + rawKey.substring(rawKey.length() - 4);
    }

    private String randomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(KEY_CHARACTERS.charAt(RANDOM.nextInt(KEY_CHARACTERS.length())));
        }
        return sb.toString();
    }

    private void recordHistory(UUID licenseId, LicenseHistoryAction action, String description,
                               String previousValue, String newValue, String performedBy) {
        WmsLicenseHistory history = WmsLicenseHistory.builder()
                .licenseId(licenseId)
                .action(action)
                .description(description)
                .previousValue(previousValue)
                .newValue(newValue)
                .performedBy(performedBy != null ? performedBy : "SYSTEM")
                .performedAt(OffsetDateTime.now())
                .build();
        wmsLicenseHistoryRepositoryPort.save(history);
    }

    private String buildStateJson(WmsLicense license) {
        if (license == null) return null;
        return String.format("{\"plan\":\"%s\",\"maxUsers\":%d,\"maxConcurrentUsers\":%d,\"adminStatus\":\"%s\",\"enabledModules\":%s}",
                license.getPlan(),
                license.getMaxUsers() != null ? license.getMaxUsers() : 0,
                license.getMaxConcurrentUsers() != null ? license.getMaxConcurrentUsers() : 0,
                license.getAdminStatus(),
                license.getEnabledModules() != null ? license.getEnabledModules().toString() : "[]");
    }

    private Map<String, Object> buildStateMap(WmsLicense license) {
        if (license == null) return null;
        Map<String, Object> map = new HashMap<>();
        map.put("id", license.getId() != null ? license.getId().toString() : null);
        map.put("organizationId", license.getOrganizationId() != null ? license.getOrganizationId().toString() : null);
        map.put("licenseName", license.getLicenseName());
        map.put("plan", license.getPlan() != null ? license.getPlan().name() : null);
        map.put("adminStatus", license.getAdminStatus() != null ? license.getAdminStatus().name() : null);
        map.put("maxUsers", license.getMaxUsers());
        map.put("enabledModules", license.getEnabledModules() != null ? license.getEnabledModules().toString() : null);
        return map;
    }

    private void logAuditChange(String username, String action, UUID entityId, Map<String, Object> before, Map<String, Object> after) {
        try {
            UserEntity actor = userRepositoryPort.findByUsername(username).orElse(null);
            if (actor != null) {
                auditService.log(actor, action, "WMS_LICENSE", entityId, before, after);
            }
        } catch (Exception e) {
            log.error("Failed to persist audit log for WMS license operation", e);
        }
    }
}
