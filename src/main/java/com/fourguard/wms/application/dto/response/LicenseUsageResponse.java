package com.fourguard.wms.application.dto.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LicenseUsageResponse {
    private UUID licenseId;
    private Long currentUsers;
    private Long concurrentUsersPeak;
    private Long currentWarehouses;
    private Long registeredHandheldDevices;
    private Long activeIntegrations;
}
