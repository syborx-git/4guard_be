package com.fourguard.wms.application.dto.response.reception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UaMappingResponse {
    private UUID id;
    private UUID receptionId;
    private UUID palletId;
    private String supplierUaCode;
    private String internalUaCode;
    private String relabelledBy;
    private OffsetDateTime relabelledAt;
    private String reason;
}
