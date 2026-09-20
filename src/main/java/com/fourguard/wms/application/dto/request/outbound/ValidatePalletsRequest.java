package com.fourguard.wms.application.dto.request.outbound;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for batch validation of scanned pallet barcodes/SSCCs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidatePalletsRequest {

    @NotNull(message = "organizationId es obligatorio")
    private UUID organizationId;

    private UUID branchId;

    @NotEmpty(message = "Debe proporcionar al menos un código de barras / SSCC")
    private List<String> barcodes;
}
