package com.fourguard.wms.infrastructure.persistence.adapter;

import com.fourguard.wms.domain.enums.LicenseAdminStatus;
import com.fourguard.wms.domain.model.LicenseUsage;
import com.fourguard.wms.domain.model.WmsLicense;
import com.fourguard.wms.domain.model.WmsLicenseHistory;
import com.fourguard.wms.domain.ports.out.WmsLicenseHistoryRepositoryPort;
import com.fourguard.wms.domain.ports.out.WmsLicenseRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.LicenseUsageViewEntity;
import com.fourguard.wms.infrastructure.persistence.entity.OrganizationEntity;
import com.fourguard.wms.infrastructure.persistence.entity.WmsLicenseEntity;
import com.fourguard.wms.infrastructure.persistence.entity.WmsLicenseHistoryEntity;
import com.fourguard.wms.infrastructure.persistence.repository.LicenseUsageJpaRepository;
import com.fourguard.wms.infrastructure.persistence.repository.OrganizationJpaRepository;
import com.fourguard.wms.infrastructure.persistence.repository.WmsLicenseHistoryJpaRepository;
import com.fourguard.wms.infrastructure.persistence.repository.WmsLicenseJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class WmsLicenseRepositoryAdapter implements WmsLicenseRepositoryPort, WmsLicenseHistoryRepositoryPort {

    private final WmsLicenseJpaRepository wmsLicenseJpaRepository;
    private final WmsLicenseHistoryJpaRepository wmsLicenseHistoryJpaRepository;
    private final LicenseUsageJpaRepository licenseUsageJpaRepository;
    private final OrganizationJpaRepository organizationJpaRepository;

    @Override
    public WmsLicense save(WmsLicense license) {
        WmsLicenseEntity entity = toEntity(license);
        WmsLicenseEntity saved = wmsLicenseJpaRepository.save(entity);
        return toDomainModel(saved);
    }

    @Override
    public Optional<WmsLicense> findById(UUID id) {
        return wmsLicenseJpaRepository.findById(id).map(this::toDomainModel);
    }

    @Override
    public List<WmsLicense> findAll() {
        return wmsLicenseJpaRepository.findAll().stream()
                .map(this::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public List<WmsLicense> findByOrganizationId(UUID organizationId) {
        return wmsLicenseJpaRepository.findByOrganizationId(organizationId).stream()
                .map(this::toDomainModel)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<LicenseUsage> findUsageByLicenseId(UUID licenseId) {
        return licenseUsageJpaRepository.findById(licenseId).map(this::toUsageDomainModel);
    }

    @Override
    public boolean existsByOrganizationIdAndAdminStatus(UUID organizationId, LicenseAdminStatus status) {
        return wmsLicenseJpaRepository.existsByOrganizationIdAndAdminStatus(organizationId, status);
    }

    @Override
    public WmsLicenseHistory save(WmsLicenseHistory history) {
        WmsLicenseHistoryEntity entity = toHistoryEntity(history);
        WmsLicenseHistoryEntity saved = wmsLicenseHistoryJpaRepository.save(entity);
        return toHistoryDomainModel(saved);
    }

    @Override
    public List<WmsLicenseHistory> findByLicenseIdOrderByPerformedAtDesc(UUID licenseId) {
        return wmsLicenseHistoryJpaRepository.findByLicenseIdOrderByPerformedAtDesc(licenseId).stream()
                .map(this::toHistoryDomainModel)
                .collect(Collectors.toList());
    }

    // ── Mapping Helpers ───────────────────────────────────────────────────────

    private WmsLicense toDomainModel(WmsLicenseEntity entity) {
        if (entity == null) return null;
        return WmsLicense.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganization() != null ? entity.getOrganization().getId() : null)
                .organizationName(entity.getOrganizationName())
                .licenseName(entity.getLicenseName())
                .licenseKeyHash(entity.getLicenseKeyHash())
                .maskedLicenseKey(entity.getMaskedLicenseKey())
                .plan(entity.getPlan())
                .description(entity.getDescription())
                .validFrom(entity.getValidFrom())
                .validUntil(entity.getValidUntil())
                .gracePeriodDays(entity.getGracePeriodDays())
                .autoRenewal(entity.getAutoRenewal())
                .adminStatus(entity.getAdminStatus())
                .maxUsers(entity.getMaxUsers())
                .maxConcurrentUsers(entity.getMaxConcurrentUsers())
                .maxWarehouses(entity.getMaxWarehouses())
                .maxHandheldDevices(entity.getMaxHandheldDevices())
                .maxIntegrations(entity.getMaxIntegrations())
                .enabledModules(entity.getEnabledModules() != null ? new ArrayList<>(entity.getEnabledModules()) : new ArrayList<>())
                .administrativeReason(entity.getAdministrativeReason())
                .observations(entity.getObservations())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }

    private WmsLicenseEntity toEntity(WmsLicense domain) {
        if (domain == null) return null;
        OrganizationEntity orgRef = null;
        if (domain.getOrganizationId() != null) {
            orgRef = organizationJpaRepository.getReferenceById(domain.getOrganizationId());
        }

        return WmsLicenseEntity.builder()
                .id(domain.getId())
                .organization(orgRef)
                .organizationName(domain.getOrganizationName())
                .licenseName(domain.getLicenseName())
                .licenseKeyHash(domain.getLicenseKeyHash())
                .maskedLicenseKey(domain.getMaskedLicenseKey())
                .plan(domain.getPlan())
                .description(domain.getDescription())
                .validFrom(domain.getValidFrom())
                .validUntil(domain.getValidUntil())
                .gracePeriodDays(domain.getGracePeriodDays())
                .autoRenewal(domain.getAutoRenewal())
                .adminStatus(domain.getAdminStatus())
                .maxUsers(domain.getMaxUsers())
                .maxConcurrentUsers(domain.getMaxConcurrentUsers())
                .maxWarehouses(domain.getMaxWarehouses())
                .maxHandheldDevices(domain.getMaxHandheldDevices())
                .maxIntegrations(domain.getMaxIntegrations())
                .enabledModules(domain.getEnabledModules() != null ? new ArrayList<>(domain.getEnabledModules()) : new ArrayList<>())
                .administrativeReason(domain.getAdministrativeReason())
                .observations(domain.getObservations())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .updatedBy(domain.getUpdatedBy() != null ? domain.getUpdatedBy() : "SYSTEM")
                .build();
    }

    private WmsLicenseHistory toHistoryDomainModel(WmsLicenseHistoryEntity entity) {
        if (entity == null) return null;
        return WmsLicenseHistory.builder()
                .id(entity.getId())
                .licenseId(entity.getLicense() != null ? entity.getLicense().getId() : null)
                .action(entity.getAction())
                .description(entity.getDescription())
                .previousValue(entity.getPreviousValue())
                .newValue(entity.getNewValue())
                .performedBy(entity.getPerformedBy())
                .performedAt(entity.getPerformedAt())
                .build();
    }

    private WmsLicenseHistoryEntity toHistoryEntity(WmsLicenseHistory domain) {
        if (domain == null) return null;
        WmsLicenseEntity licenseRef = null;
        if (domain.getLicenseId() != null) {
            licenseRef = wmsLicenseJpaRepository.getReferenceById(domain.getLicenseId());
        }

        return WmsLicenseHistoryEntity.builder()
                .id(domain.getId())
                .license(licenseRef)
                .action(domain.getAction())
                .description(domain.getDescription())
                .previousValue(domain.getPreviousValue())
                .newValue(domain.getNewValue())
                .performedBy(domain.getPerformedBy() != null ? domain.getPerformedBy() : "SYSTEM")
                .performedAt(domain.getPerformedAt())
                .build();
    }

    private LicenseUsage toUsageDomainModel(LicenseUsageViewEntity view) {
        if (view == null) return null;
        return LicenseUsage.builder()
                .licenseId(view.getLicenseId())
                .currentUsers(view.getCurrentUsers() != null ? view.getCurrentUsers() : 0L)
                .concurrentUsersPeak(view.getConcurrentUsersPeak() != null ? view.getConcurrentUsersPeak() : 0L)
                .currentWarehouses(view.getCurrentWarehouses() != null ? view.getCurrentWarehouses() : 0L)
                .registeredHandheldDevices(view.getRegisteredHandheldDevices() != null ? view.getRegisteredHandheldDevices() : 0L)
                .activeIntegrations(view.getActiveIntegrations() != null ? view.getActiveIntegrations() : 0L)
                .build();
    }
}
