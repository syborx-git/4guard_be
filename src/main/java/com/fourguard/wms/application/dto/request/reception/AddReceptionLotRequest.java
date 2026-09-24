package com.fourguard.wms.application.dto.request.reception;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddReceptionLotRequest {

    @NotBlank(message = "El número de lote es obligatorio")
    private String lotNumber;

    private UUID skuId;

    private LocalDate elaborationDate;

    @NotNull(message = "La fecha de caducidad es obligatoria")
    private LocalDate expirationDate;

    private String observations;
}
