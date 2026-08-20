package com.fourguard.wms.application.dto.request.outbound;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for creating an Outbound dispatch (Salida de Almacén F03).
 */
@Data
public class CreateOutboundRequest {

    @NotNull(message = "organizationId es obligatorio")
    private UUID organizationId;

    @NotNull(message = "branchId es obligatorio")
    private UUID branchId;

    @NotNull(message = "clientId es obligatorio")
    private UUID clientId;

    private UUID destinationId;
    private String destinationName;
    private String destinationAddress;

    private UUID carrierId;
    private String carrierName;

    @NotBlank(message = "transportType es obligatorio (CAMION, TORTON, TRAILER)")
    private String transportType;

    @NotBlank(message = "driverName es obligatorio")
    private String driverName;

    private String economicNumber;

    @NotBlank(message = "tractorPlates es obligatorio")
    private String tractorPlates;

    @NotBlank(message = "boxPlates es obligatorio")
    private String boxPlates;

    @NotBlank(message = "sealNumber es obligatorio")
    private String sealNumber;

    @NotBlank(message = "remisionNo es obligatorio")
    private String remisionNo;

    @NotEmpty(message = "Se requiere al menos una tarima / item para despachar")
    private List<UUID> selectedItemIds;
}
