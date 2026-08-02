package com.fourguard.wms.domain.ports.out;

import com.fourguard.wms.domain.enums.LicenseAdminStatus;
import com.fourguard.wms.domain.model.LicenseUsage;
import com.fourguard.wms.domain.model.WmsLicense;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WmsLicenseRepositoryPort {
    WmsLicense save(WmsLicense license);
    Optional<WmsLicense> findById(UUID id);
    List<WmsLicense> findAll();
    List<WmsLicense> findByOrganizationId(UUID organizationId);
    Optional<LicenseUsage> findUsageByLicenseId(UUID licenseId);
    boolean existsByOrganizationIdAndAdminStatus(UUID organizationId, LicenseAdminStatus status);
}
