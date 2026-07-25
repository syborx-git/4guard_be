package com.fourguard.wms.application.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Response DTO for Product SKU. */
@Getter
@Builder
public class ProductSkuResponse {
    private final UUID id;
    private final UUID clientId;
    private final String clientName;
    private final String code;
    private final String name;
    private final String description;
    private final BigDecimal weight;
    private final String unit;
    private final String status;
    private final Boolean isDeleted;
    private final Long version;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;
}
