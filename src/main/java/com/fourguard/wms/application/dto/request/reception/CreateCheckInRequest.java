package com.fourguard.wms.application.dto.request.reception;

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

    /** Optional: Assigned forklift operator UUID */
    private UUID forkliftOperatorId;

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

    /** List of security seal numbers. At least one recommended. */
    private List<String> sealNumbers;
}
