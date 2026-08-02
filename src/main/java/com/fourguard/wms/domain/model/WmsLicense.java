package com.fourguard.wms.domain.model;

import com.fourguard.wms.domain.enums.LicenseAdminStatus;
import com.fourguard.wms.domain.enums.LicensePlan;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class WmsLicense {
    private UUID id;
    private UUID organizationId;
    private String organizationName;
    private String licenseName;
    private String licenseKeyHash;
    private String maskedLicenseKey;
    private LicensePlan plan;
    private String description;
    private OffsetDateTime validFrom;
    private OffsetDateTime validUntil;
    private Integer gracePeriodDays;
    private Boolean autoRenewal;
    private LicenseAdminStatus adminStatus;
    
    // Limits
    private Integer maxUsers;
    private Integer maxConcurrentUsers;
    private Integer maxWarehouses;
    private Integer maxHandheldDevices;
    private Integer maxIntegrations;
    
    // Modules
    private List<String> enabledModules;
    
    private String administrativeReason;
    private String observations;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private String updatedBy;
}
