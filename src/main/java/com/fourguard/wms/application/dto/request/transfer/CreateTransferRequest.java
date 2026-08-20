package com.fourguard.wms.application.dto.request.transfer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for creating an internal warehouse transfer (Cambio de Almacén).
 */
@Data
public class CreateTransferRequest {

    @NotNull(message = "organizationId es obligatorio")
    private UUID organizationId;

    @NotNull(message = "branchId es obligatorio")
    private UUID branchId;

    @NotNull(message = "originLocationId es obligatorio")
    private UUID originLocationId;

    @NotNull(message = "destinationLocationId es obligatorio")
    private UUID destinationLocationId;

    private UUID forkliftOperatorId;

    @NotBlank(message = "reasonCode es obligatorio")
    private String reasonCode;

    private String reasonLabel;
    private String observations;

    @NotEmpty(message = "Se debe seleccionar al menos una tarima / item para trasladar")
    private List<UUID> selectedItemIds;
}
