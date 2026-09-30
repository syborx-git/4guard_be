package com.fourguard.wms.application.dto.request.reception;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for selective re-labelling of pallets with internal SSCC GS1-128 UAs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RelabelUasRequest {

    @NotEmpty(message = "Debe especificar al menos una tarima para re-etiquetar.")
    private List<UUID> palletIds;

    private String reason;
}
