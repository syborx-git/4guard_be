package com.fourguard.wms.application.dto.request.security;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO submitted by the Security Guard to validate, assign ramp, and
 * authorize entrance.
 */
@Data
public class GuardCheckinCompletionRequest {

    /** CARGA | DESCARGA */
    private String operationType;

    /**
     * Optional: Ramp number assigned in Caseta or pending assignment by Warehouse
     */
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
    private String driverPhone;
    private String tractorPlates;
    private String noEcoTractor;
    private String economicNumber;
    private String boxPlates;
    private String boxEconomicNumber;
    private String noEcoCaja;
    private String boxDimensions;
    private String transportType;

    private String docNumber;
    private LocalDate docDate;
    private LocalTime receptionTime;

    private List<String> sealNumbers;
    private String observations;
    private String guardNotes;
}
