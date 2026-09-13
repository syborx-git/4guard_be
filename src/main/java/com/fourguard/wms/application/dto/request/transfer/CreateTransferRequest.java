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

    private UUID organizationId;
    private UUID branchId;

    private UUID originLocationId;
    private String originLocationCode;

    private UUID destinationLocationId;
    private String destinationLocationCode;

    private UUID forkliftOperatorId;
    private String forkliftOperatorName;

    @NotBlank(message = "reasonCode es obligatorio")
    private String reasonCode;

    private String reasonLabel;
    private String observations;
    private String transferredBy;

    private List<UUID> selectedItemIds;
    private List<String> palletCodes;
    private List<String> palletIds;
}
