package com.fourguard.wms.application.dto.request.reception;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for Caseta de Seguridad check-in (pre-reception creation).
 */
@Data
public class CreateCheckInRequest {

    @NotNull(message = "organizationId es obligatorio")
    private UUID organizationId;

    @NotNull(message = "branchId es obligatorio")
    private UUID branchId;

    /** Optional: Carrier UUID from catalog */
    private UUID carrierId;

    @NotNull(message = "clientId es obligatorio")
    private UUID clientId;

    /** Optional: Ramp/andén location UUID */
    private UUID rampId;

    /** Optional: Ramp number (1-12) for fallback resolution */
    private Integer rampNumber;

    /** Optional: Ramp code (e.g. LOC-RAMP-01 or R-01) */
    private String rampCode;

    /** Optional: Assigned forklift operator UUID */
    private UUID forkliftOperatorId;

    /** Optional: Lot number captured at check-in */
    private String lotNumber;

    /** Optional: Elaboration/Manufacturing date */
    private LocalDate elaborationDate;

    /** Optional: Expiration date */
    private LocalDate expirationDate;

    @NotBlank(message = "Número de remisión / documento es obligatorio")
    private String docNumber;

    @NotNull(message = "Fecha de documento es obligatoria")
    private LocalDate docDate;

    @NotNull(message = "Hora de recepción es obligatoria")
    private LocalTime receptionTime;

    @NotBlank(message = "Nombre del chofer es obligatorio")
    private String driverName;

    @NotBlank(message = "Placas del tracto son obligatorias")
    private String tractorPlates;

    @NotBlank(message = "Placas de la caja son obligatorias")
    private String boxPlates;

    /** List of security seal numbers. At least one is mandatory. */
    @NotNull(message = "El registro de sellos de seguridad es obligatorio")
    @NotEmpty(message = "Debe registrar al menos un sello de seguridad (cincho)")
    private List<@NotBlank(message = "El número de sello no puede estar vacío") String> sealNumbers;
}
