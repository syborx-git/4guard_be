package com.fourguard.wms.application.dto.response;

import com.fourguard.wms.domain.enums.LicenseHistoryAction;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WmsLicenseHistoryResponse {
    private UUID id;
    private UUID licenseId;
    private LicenseHistoryAction action;
    private String description;
    private String previousValue;
    private String newValue;
    private String performedBy;
    private OffsetDateTime performedAt;
}
