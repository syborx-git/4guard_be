package com.fourguard.wms.application.dto.response;

import com.fourguard.wms.domain.enums.CurrencyStatus;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CurrencyResponse {

    private UUID id;
    private UUID organizationId;
    private String code;
    private String name;
    private String symbol;
    private Boolean isBase;
    private CurrencyStatus status;
    private Integer decimalPlaces;
    private OffsetDateTime createdAt;
    private String createdBy;
    private OffsetDateTime updatedAt;
    private String updatedBy;
}
