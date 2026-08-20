package com.fourguard.wms.application.dto.response.transfer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Full detail response DTO for internal warehouse transfer (Cambio de Almacén).
 * Homologated with Frontend `WarehouseTransfer`.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferResponse {

    private UUID id;
    private UUID organizationId;
    private UUID branchId;
    private String folio;
    private String status;

    private UUID originLocationId;
    private String originLocationCode;
    private UUID destinationLocationId;
    private String destinationLocationCode;

    private UUID forkliftOperatorId;
    private String forkliftOperatorName;

    private String reasonCode;
    private String reasonLabel;
    private String observations;

    private Integer totalPallets;
    private Double totalPieces;
    private Integer distinctSkus;

    private List<TransferItemResponse> items;

    private OffsetDateTime cancelledAt;
    private String cancellationReason;
    private String cancelledBy;

    private Long version;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
