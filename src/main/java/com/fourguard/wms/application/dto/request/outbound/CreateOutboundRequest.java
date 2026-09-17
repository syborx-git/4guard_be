package com.fourguard.wms.application.dto.request.outbound;

import jakarta.validation.constraints.NotBlank;
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

    /** Client UUID from catalog (or resolved via clientCode/clientName) */
    private UUID clientId;
    private String clientCode;
    private String clientName;

    private UUID destinationId;
    private String destinationName;
    private String destinationAddress;

    private UUID carrierId;
    private String carrierName;
    private String carrierLineCode;
    private String carrierLine;

    private UUID rampId;
    private Integer rampNumber;
    private String rampCode;

    private UUID forkliftOperatorId;
    private String forkliftOperatorName;

    @NotBlank(message = "transportType es obligatorio (CAMION, TORTON, TRAILER)")
    private String transportType;

    @NotBlank(message = "driverName es obligatorio")
    private String driverName;

    private String economicNumber;
    private String boxEconomicNumber;

    @NotBlank(message = "tractorPlates es obligatorio")
    private String tractorPlates;

    @NotBlank(message = "boxPlates es obligatorio")
    private String boxPlates;

    private String sealNumber;

    private String remisionNo;

    private String observations;

    private String status;

    private List<UUID> selectedItemIds;
}
