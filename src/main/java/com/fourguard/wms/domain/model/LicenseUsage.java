package com.fourguard.wms.domain.model;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class LicenseUsage {
    private UUID licenseId;
    private Long currentUsers;
    private Long concurrentUsersPeak;
    private Long currentWarehouses;
    private Long registeredHandheldDevices;
    private Long activeIntegrations;
}
