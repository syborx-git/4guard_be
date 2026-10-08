package com.fourguard.wms.application.dto.request.reception;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO for reopening a completed warehouse reception (F01).
 * Requires admin / supervisor authorization credentials and mandatory reason.
 */
@Data
public class ReopenReceptionRequest {

    @NotBlank(message = "adminUsername es obligatorio")
    private String adminUsername;

    @NotBlank(message = "adminPassword es obligatorio")
    private String adminPassword;

    @NotBlank(message = "El motivo de reapertura es obligatorio")
    private String reason;
}
