package com.fourguard.wms.application.dto.request.outbound;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO for cancelling an Outbound dispatch.
 */
@Data
public class CancelOutboundRequest {

    @NotBlank(message = "adminUsername es obligatorio")
    private String adminUsername;

    @NotBlank(message = "adminPassword es obligatorio")
    private String adminPassword;

    @NotBlank(message = "El motivo de cancelación es obligatorio")
    private String reason;
}
