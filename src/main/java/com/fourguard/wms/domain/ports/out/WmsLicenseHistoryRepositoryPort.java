package com.fourguard.wms.domain.ports.out;

import com.fourguard.wms.domain.model.WmsLicenseHistory;

import java.util.List;
import java.util.UUID;

public interface WmsLicenseHistoryRepositoryPort {
    WmsLicenseHistory save(WmsLicenseHistory history);
    List<WmsLicenseHistory> findByLicenseIdOrderByPerformedAtDesc(UUID licenseId);
}
