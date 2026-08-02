package com.fourguard.wms.application.dto.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LicenseKeyGeneratedResponse {
    private UUID licenseId;
    private String rawLicenseKey;
    private String maskedLicenseKey;
    private String message;
}
