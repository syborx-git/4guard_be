package com.fourguard.wms.application.mapper;

import com.fourguard.wms.application.dto.request.CreateLicenseRequest;
import com.fourguard.wms.application.dto.response.LicenseUsageResponse;
import com.fourguard.wms.application.dto.response.WmsLicenseHistoryResponse;
import com.fourguard.wms.application.dto.response.WmsLicenseResponse;
import com.fourguard.wms.domain.model.LicenseUsage;
import com.fourguard.wms.domain.model.WmsLicense;
import com.fourguard.wms.domain.model.WmsLicenseHistory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class WmsLicenseMapper {

    public WmsLicenseResponse toResponse(WmsLicense domain) {
        if (domain == null) return null;
        return WmsLicenseResponse.builder()
                .id(domain.getId())
                .organizationId(domain.getOrganizationId())
                .organizationName(domain.getOrganizationName())
                .licenseName(domain.getLicenseName())
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
                .updatedBy(domain.getUpdatedBy())
                .build();
    }

    public WmsLicense toDomainModel(CreateLicenseRequest request) {
        if (request == null) return null;
        return WmsLicense.builder()
                .organizationId(request.getOrganizationId())
                .licenseName(request.getLicenseName())
                .plan(request.getPlan())
                .description(request.getDescription())
                .validFrom(request.getValidFrom())
                .validUntil(request.getValidUntil())
                .gracePeriodDays(request.getGracePeriodDays() != null ? request.getGracePeriodDays() : 15)
                .autoRenewal(request.getAutoRenewal() != null ? request.getAutoRenewal() : false)
                .maxUsers(request.getMaxUsers() != null ? request.getMaxUsers() : 10)
                .maxConcurrentUsers(request.getMaxConcurrentUsers() != null ? request.getMaxConcurrentUsers() : 5)
                .maxWarehouses(request.getMaxWarehouses() != null ? request.getMaxWarehouses() : 1)
                .maxHandheldDevices(request.getMaxHandheldDevices() != null ? request.getMaxHandheldDevices() : 5)
                .maxIntegrations(request.getMaxIntegrations() != null ? request.getMaxIntegrations() : 1)
                .enabledModules(request.getEnabledModules() != null && !request.getEnabledModules().isEmpty() ?
                        new ArrayList<>(request.getEnabledModules()) : new ArrayList<>(List.of("WMS_CORE")))
                .observations(request.getObservations())
                .build();
    }

    public LicenseUsageResponse toUsageResponse(LicenseUsage usage) {
        if (usage == null) return null;
        return LicenseUsageResponse.builder()
                .licenseId(usage.getLicenseId())
                .currentUsers(usage.getCurrentUsers())
                .concurrentUsersPeak(usage.getConcurrentUsersPeak())
                .currentWarehouses(usage.getCurrentWarehouses())
                .registeredHandheldDevices(usage.getRegisteredHandheldDevices())
                .activeIntegrations(usage.getActiveIntegrations())
                .build();
    }

    public WmsLicenseHistoryResponse toHistoryResponse(WmsLicenseHistory history) {
        if (history == null) return null;
        return WmsLicenseHistoryResponse.builder()
                .id(history.getId())
                .licenseId(history.getLicenseId())
                .action(history.getAction())
                .description(history.getDescription())
                .previousValue(history.getPreviousValue())
                .newValue(history.getNewValue())
                .performedBy(history.getPerformedBy())
                .performedAt(history.getPerformedAt())
                .build();
    }
}
