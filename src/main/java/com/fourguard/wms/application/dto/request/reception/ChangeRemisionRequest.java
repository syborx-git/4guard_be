package com.fourguard.wms.application.dto.request.reception;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO for changing the reception remision/document number with justification
 * and high-rank (Supervisor / Admin) authorization credentials.
 */
@Data
public class ChangeRemisionRequest {

    @NotBlank(message = "El nuevo número de remisión / documento es obligatorio")
    private String newDocNumber;

    @NotBlank(message = "La justificación del cambio es obligatoria")
    private String reason;

    @NotBlank(message = "adminUsername es obligatorio")
    private String adminUsername;

    @NotBlank(message = "adminPassword es obligatorio")
    private String adminPassword;
}

