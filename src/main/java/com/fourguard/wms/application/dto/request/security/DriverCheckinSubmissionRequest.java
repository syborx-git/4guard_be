package com.fourguard.wms.application.dto.request.security;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Request DTO submitted by the carrier/driver from their mobile device.
 * Corresponds to Official Format F01-PO-CP-7.1.3-03.
 */
@Data
public class DriverCheckinSubmissionRequest {

    /** CARGA | DESCARGA */
    @NotBlank(message = "El tipo de operación es obligatorio (CARGA o DESCARGA)")
    private String operationType;

    // Documentos
    private String docNumber;
    private String noCartaPorte;
    private String remision;
    private LocalDate docDate;
    private LocalTime receptionTime;

    // Cliente y Transportista
    private String clientCode;
    private String clientName;
    private String carrierLineCode;
    private String carrierLine;

    // Datos del Operador y Unidad
    @NotBlank(message = "El nombre del operador / chofer es obligatorio")
    private String driverName;
    private String driverLicense;
    private String driverPhone;

    @NotBlank(message = "Las placas del tracto son obligatorias")
    private String tractorPlates;

    private String noEcoTractor;
    private String boxPlates;
    private String boxDimensions;
    private String transportType;

    // Sellos de Seguridad
    private List<String> sealNumbers;

    // Checklist F01-PO-CP-7.1.3-03 (JSON String or individual checklist fields)
    private String checklistData;
    private String observations;

    // Firma Digital del Transportista (Base64 signature canvas or true acknowledgement)
    private String driverSignature;
}
