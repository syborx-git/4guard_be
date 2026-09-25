package com.fourguard.wms.application.dto.response.security;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class PassResponse {

    private UUID id;
    private String token;
    private UUID organizationId;
    private UUID branchId;
    private String status; // PENDING_DRIVER, SUBMITTED, COMPLETED, CANCELLED
    private String operationType; // CARGA, DESCARGA

    // Pre-filled / Submitted info
    private UUID clientId;
    private String clientCode;
    private String clientName;

    private UUID carrierId;
    private String carrierLineCode;
    private String carrierLine;

    private String driverName;
    private String driverLicense;
    private String driverPhone;
    private String transportType;
    private String economicNumber;
    private String boxEconomicNumber;
    private String tractorPlates;
    private String boxPlates;
    private String boxDimensions;
    private List<String> sealNumbers;

    private String docNumber;
    private LocalDate docDate;
    private LocalTime receptionTime;
    private LocalTime departureTime;

    private String checklistData;
    private String observations;
    private String driverSignature;
    private OffsetDateTime driverSignedAt;

    // Resolution
    private UUID rampId;
    private Integer rampNumber;
    private String rampCode;
    private String generatedFolio;
    private String processedBy;
    private OffsetDateTime processedAt;

    // Check-Out & Exit
    private String exitObservations;
    private List<String> exitSealNumbers;
    private String exitedBy;
    private OffsetDateTime exitedAt;
    private String warehouseStatus;
    private Boolean isReadyForExit;

    private OffsetDateTime expiresAt;
    private OffsetDateTime createdAt;
}
