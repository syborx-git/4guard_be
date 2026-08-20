package com.fourguard.wms.application.dto.response.outbound;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response DTO for an individual item dispatched in an outbound movement.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboundItemResponse {

    private UUID id;
    private UUID itemId;
    private String palletCode;
    private String skuCode;
    private String skuDescription;
    private String lotNumber;
    private LocalDate expirationDate;
    private String locationCode;
    private Double pieces;
    private OffsetDateTime createdAt;
}
