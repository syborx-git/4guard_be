package com.fourguard.wms.application.dto.response.outbound;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Summary DTO for outbound dispatch list views.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboundSummaryResponse {

    private UUID id;
    private String folio;
    private String status;

    private UUID clientId;
    private String clientName;
    private UUID destinationId;
    private String destinationName;

    private UUID carrierId;
    private String carrierName;
    private String transportType;
    private String driverName;
    private String tractorPlates;
    private String boxPlates;
    private String sealNumber;
    private String remisionNo;

    private Integer totalPallets;
    private Double totalPieces;
    private Integer distinctSkus;

    private OffsetDateTime createdAt;
    private String createdBy;
}
