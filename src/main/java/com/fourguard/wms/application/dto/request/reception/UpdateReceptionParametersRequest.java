package com.fourguard.wms.application.dto.request.reception;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for updating andén parameters and caseta data of a reception.
 */
@Data
public class UpdateReceptionParametersRequest {

    private UUID skuId;
    private String skuCode;
    private String productName;
    private UUID supplierId;
    private String supplierName;
    private String lotNumber;
    private LocalDate elaborationDate;
    private LocalDate expirationDate;

    private Double piecesPerPallet;

    /** Must match PalletType enum values */
    private String palletType;

    private UUID storageLocationId;
    private String storageLocationCode;

    /** Optional: Assigned forklift operator */
    private UUID forkliftOperatorId;
    private String forkliftOperatorName;

    /** Optional: Assigned ramp */
    private UUID rampId;
    private Integer rampNumber;
    private String rampCode;

    /** Optional: Target lifecycle status (ASSIGNED, IN_PROGRESS, DISCHARGED) */
    private String status;

    private String observations;
    private String authorizedByOpsManager;
    private String opsManagerReason;

    // ── Caseta / Transport Data Updates ──
    private String tractorPlates;
    private String boxPlates;
    private String economicNumber;
    private String noEcoTractor;
    private String boxEconomicNumber;
    private String noEcoCaja;
    private String driverName;
    private String docNumber;
    private LocalDate docDate;
    private LocalTime receptionTime;

    private UUID carrierId;
    private String carrierLineCode;
    private String carrierLine;

    private UUID clientId;
    private String clientCode;
    private String clientName;

    private List<String> sealNumbers;
}

