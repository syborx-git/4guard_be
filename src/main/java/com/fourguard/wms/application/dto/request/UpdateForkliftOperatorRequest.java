package com.fourguard.wms.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Request DTO for updating an existing Forklift Operator (HU-142).
 * The {@code id} must match the path variable in the controller.
 */
@Value
@Builder
public class UpdateForkliftOperatorRequest {

    @NotNull(message = "El ID del montacarguista es requerido para la actualización")
    @Schema(description = "UUID del montacarguista a actualizar", example = "d45f0907-9fa5-4bdf-87db-2eb5e7683950")
    UUID id;

    @NotNull(message = "El ID de la organización es requerido")
    @Schema(description = "UUID de la organización propietaria", example = "a53f0907-9fa5-4bdf-87db-2eb5e7683935")
    UUID organizationId;

    @Schema(description = "UUID de la sucursal (opcional)", example = "b73f0907-9fa5-4bdf-87db-2eb5e7683936")
    UUID branchId;

    @NotBlank(message = "El nombre(s) del operador es requerido")
    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    @Schema(description = "Nombre(s) del montacarguista", example = "Juan Manuel")
    String firstName;

    @NotBlank(message = "El apellido paterno es requerido")
    @Size(max = 100, message = "El apellido paterno no puede superar 100 caracteres")
    @Schema(description = "Apellido paterno del montacarguista", example = "Pérez")
    String lastNamePaternal;

    @NotBlank(message = "El apellido materno es requerido")
    @Size(max = 100, message = "El apellido materno no puede superar 100 caracteres")
    @Schema(description = "Apellido materno del montacarguista", example = "Hernández")
    String lastNameMaternal;

    @NotBlank(message = "El número de licencia DC-3 es requerido")
    @Size(max = 50, message = "El número de licencia DC-3 no puede superar 50 caracteres")
    @Schema(description = "Número de certificación DC-3 registrado ante STPS", example = "LIC-MC-9921")
    String licenseNumberDc3;

    @NotNull(message = "La fecha de vencimiento de la licencia es requerida")
    @Schema(description = "Fecha de vencimiento de la certificación DC-3 (YYYY-MM-DD)", example = "2027-06-30")
    LocalDate licenseExpirationDate;

    @Schema(description = "UUID del turno asignado del catálogo maestro de turnos", example = "c12f0907-9fa5-4bdf-87db-2eb5e7683940")
    UUID shiftId;

    @Schema(description = "Versión actual del registro para optimistic locking", example = "1")
    Long version;
}
