package com.fourguard.wms.application.dto.request.reception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * Request DTO for adding one or more pallets/UAs to an open reception.
 */
@Data
public class AddReceptionPalletsRequest {

    @NotEmpty(message = "Se requiere al menos una tarima")
    @Valid
    private List<PalletItemRequest> pallets;

    @Data
    public static class PalletItemRequest {

        @NotNull(message = "palletCode (UA / SSCC) es obligatorio")
        private String palletCode;

        @NotNull(message = "pieces es obligatorio")
        private Double pieces;

        /** Optional override — falls back to reception-level pallet_type */
        private String palletType;

        private String observations;
    }
}
