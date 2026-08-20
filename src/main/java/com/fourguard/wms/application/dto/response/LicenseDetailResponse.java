package com.fourguard.wms.application.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LicenseDetailResponse {
    private WmsLicenseResponse license;
    private LicenseUsageResponse usage;
}
