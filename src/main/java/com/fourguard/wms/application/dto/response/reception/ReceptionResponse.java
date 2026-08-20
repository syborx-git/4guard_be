package com.fourguard.wms.application.dto.response.reception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Full detail Response DTO for a warehouse reception (F01).
 * Homologated with Frontend `ReceptionHeader`.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceptionResponse {

    private UUID id;
    private UUID organizationId;
    private UUID branchId;
    private String folio;
    private String status;

    // CheckIn Caseta
    private UUID carrierId;
    private String carrierName;
    private UUID clientId;
    private String clientName;
    private UUID rampId;
    private String rampName;
    private UUID forkliftOperatorId;
    private String forkliftOperatorName;
    private String docNumber;
    private LocalDate docDate;
    private LocalTime receptionTime;
    private String driverName;
    private String tractorPlates;
    private String boxPlates;
    private List<String> sealNumbers;

    // Parámetros de Descarga / Lote
    private UUID skuId;
    private String skuCode;
    private String productName;
    private UUID supplierId;
    private String supplierName;
    private String lotNumber;
    private LocalDate elaborationDate;
    private LocalDate expirationDate;
    private Double piecesPerPallet;
    private String palletType;
    private String palletTypeLabel;
    private UUID storageLocationId;
    private String storageLocationCode;
    private String observations;

    // Totales calculados
    private Integer totalPallets;
    private Double totalPieces;

    // Detalle de tarimas (UAs)
    private List<ReceptionPalletResponse> pallets;

    // Cierre y Cancelación
    private OffsetDateTime completedAt;
    private String leaderAuthorizedBy;
    private OffsetDateTime cancelledAt;
    private String cancellationReason;
    private String cancelledBy;

    // Auditoría
    private Long version;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
