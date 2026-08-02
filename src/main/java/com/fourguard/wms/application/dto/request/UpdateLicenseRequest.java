package com.fourguard.wms.application.dto.request;

import com.fourguard.wms.domain.enums.LicensePlan;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateLicenseRequest {
    private String licenseName;
    private LicensePlan plan;
    private String description;
    private OffsetDateTime validFrom;
    private OffsetDateTime validUntil;
    private Integer gracePeriodDays;
    private Boolean autoRenewal;

    private Integer maxUsers;
    private Integer maxConcurrentUsers;
    private Integer maxWarehouses;
    private Integer maxHandheldDevices;
    private Integer maxIntegrations;

    private List<String> enabledModules;

    private String administrativeReason;
    private String observations;
}
