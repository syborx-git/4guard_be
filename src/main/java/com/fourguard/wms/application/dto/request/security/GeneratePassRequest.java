package com.fourguard.wms.application.dto.request.security;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class GeneratePassRequest {

    @NotNull(message = "organizationId es obligatorio")
    private UUID organizationId;

    @NotNull(message = "branchId es obligatorio")
    private UUID branchId;

    /** CARGA | DESCARGA */
    private String operationType;

    /** Optional pre-filled data by the guard */
    private String clientCode;
    private String clientName;
    private String carrierLineCode;
    private String carrierLine;
    private String driverName;
    private String driverPhone;
    private String tractorPlates;
    private String docNumber;
}
