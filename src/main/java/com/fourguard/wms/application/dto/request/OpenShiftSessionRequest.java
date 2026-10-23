package com.fourguard.wms.application.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class OpenShiftSessionRequest {

    @NotNull(message = "La sucursal (branchId) es requerida")
    UUID branchId;

    /** Opcional. Si se informa, abre una sesión manual con ese turno. */
    UUID shiftId;

    String reason;
}
