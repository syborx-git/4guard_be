package com.fourguard.wms.application.dto.request.reception;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Request DTO for updating andén parameters of a REGISTERED reception.
 */
@Data
public class UpdateReceptionParametersRequest {

    private UUID skuId;
    private UUID supplierId;
    private String lotNumber;
    private LocalDate elaborationDate;
    private LocalDate expirationDate;

    @NotNull(message = "piecesPerPallet es obligatorio")
    private Double piecesPerPallet;

    /** Must match PalletType enum values */
    private String palletType;

    /** FK to wms.locations for the storage bahía */
    private UUID storageLocationId;

    private String observations;
}
