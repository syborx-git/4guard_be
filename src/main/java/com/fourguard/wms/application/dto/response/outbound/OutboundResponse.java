package com.fourguard.wms.application.dto.response.outbound;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Full detail response DTO for Outbound dispatch (Salida de Almacén F03).
 * Homologated with Frontend `WarehouseOutbound`.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboundResponse {

    private UUID id;
    private UUID organizationId;
    private UUID branchId;
    private String folio;
    private String status;

    private UUID clientId;
    private String clientName;
    private UUID destinationId;
    private String destinationName;
    private String destinationAddress;

    private UUID carrierId;
    private String carrierName;
    private String transportType;
    private String driverName;
    private String economicNumber;
    private String tractorPlates;
    private String boxPlates;
    private String sealNumber;
    private String remisionNo;

    private Integer totalPallets;
    private Double totalPieces;
    private Integer distinctSkus;

    private List<OutboundItemResponse> items;

    private OffsetDateTime cancelledAt;
    private String cancellationReason;
    private String cancelledBy;

    private Long version;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
