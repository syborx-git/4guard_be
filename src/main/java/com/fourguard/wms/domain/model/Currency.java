package com.fourguard.wms.domain.model;

import com.fourguard.wms.domain.enums.CurrencyStatus;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Currency {

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
