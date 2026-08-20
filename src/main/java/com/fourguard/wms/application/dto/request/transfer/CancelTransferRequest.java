package com.fourguard.wms.application.dto.request.transfer;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO for cancelling an internal warehouse transfer.
 */
@Data
public class CancelTransferRequest {

    @NotBlank(message = "adminUsername es obligatorio")
    private String adminUsername;

    @NotBlank(message = "adminPassword es obligatorio")
    private String adminPassword;

    @NotBlank(message = "El motivo de cancelación es obligatorio")
    private String reason;
}
