package com.fourguard.wms.domain.ports.in;

import com.fourguard.wms.application.dto.request.*;
import com.fourguard.wms.application.dto.response.*;

import java.util.List;
import java.util.UUID;

public interface WmsLicenseUseCase {
    WmsLicenseResponse createLicense(CreateLicenseRequest request);
    WmsLicenseResponse updateLicense(UUID id, UpdateLicenseRequest request);
    WmsLicenseResponse renewLicense(UUID id, RenewLicenseRequest request);
    WmsLicenseResponse suspendLicense(UUID id, SuspendLicenseRequest request);
    WmsLicenseResponse reactivateLicense(UUID id);
    WmsLicenseResponse revokeLicense(UUID id, String reason);
    LicenseKeyGeneratedResponse regenerateKey(UUID id);
    LicenseDetailResponse getLicenseById(UUID id);
    List<WmsLicenseResponse> getAllLicenses(UUID organizationId);
    List<WmsLicenseHistoryResponse> getLicenseHistory(UUID id);
}
