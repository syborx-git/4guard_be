package com.fourguard.wms.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Value;

/**
 * Request DTO for toggling the status of a Forklift Operator (HU-142).
 */
@Value
@Builder
public class UpdateForkliftOperatorStatusRequest {

    @NotBlank(message = "El nuevo estatus es requerido")
    @Schema(description = "Nuevo estatus del operador: ACTIVO o INACTIVO", example = "INACTIVO")
    String status;

    @Schema(description = "Motivo del cambio de estatus (opcional)", example = "Operador en periodo de incapacidad médica")
    String reason;

    @Schema(description = "Observaciones adicionales (opcional)")
    String observations;
}
