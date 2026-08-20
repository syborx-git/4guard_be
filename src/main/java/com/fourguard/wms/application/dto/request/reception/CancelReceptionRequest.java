package com.fourguard.wms.application.dto.request.reception;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO for cancelling a warehouse reception (F01).
 * Requires admin / supervisor authorization credentials and mandatory reason.
 */
@Data
public class CancelReceptionRequest {

    @NotBlank(message = "adminUsername es obligatorio")
    private String adminUsername;

    @NotBlank(message = "adminPassword es obligatorio")
    private String adminPassword;

    @NotBlank(message = "El motivo de cancelación es obligatorio")
    private String reason;
}
