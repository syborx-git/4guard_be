package com.fourguard.wms.application.dto.request.security;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO submitted by the Security Guard to validate, assign ramp, and authorize entrance.
 */
@Data
public class GuardCheckinCompletionRequest {

    /** CARGA | DESCARGA */
    private String operationType;

    @NotNull(message = "Rampa/Andén es obligatorio")
    private Integer rampNumber;
    private String rampCode;
    private UUID rampId;

    private UUID forkliftOperatorId;
    private String forkliftOperatorName;

    private String clientCode;
    private String clientName;
    private UUID clientId;

    private String carrierLineCode;
    private String carrierLine;
    private UUID carrierId;

    private String driverName;
    private String driverLicense;
    private String tractorPlates;
    private String noEcoTractor;
    private String boxPlates;
    private String boxDimensions;
    private String transportType;

    private String docNumber;
    private LocalDate docDate;
    private LocalTime receptionTime;

    private List<String> sealNumbers;
    private String observations;
    private String guardNotes;
}
