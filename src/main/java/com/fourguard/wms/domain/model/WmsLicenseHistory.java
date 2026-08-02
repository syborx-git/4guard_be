package com.fourguard.wms.domain.model;

import com.fourguard.wms.domain.enums.LicenseHistoryAction;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class WmsLicenseHistory {
    private UUID id;
    private UUID licenseId;
    private LicenseHistoryAction action;
    private String description;
    private String previousValue; // JSON string or representation
    private String newValue;      // JSON string or representation
    private String performedBy;
    private OffsetDateTime performedAt;
}
