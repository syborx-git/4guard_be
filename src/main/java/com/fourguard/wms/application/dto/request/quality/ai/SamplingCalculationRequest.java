package com.fourguard.wms.application.dto.request.quality.ai;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SamplingCalculationRequest {

    @NotBlank(message = "El tipo de material es obligatorio (ej. ETIQUETAS, TAPAS_CULINARIOS, CAJAS, CAFE_VERDE, etc.)")
    private String materialType;

    private Double lotQuantity; // Cantidad total del lote
    private Integer totalPallets; // Número total de tarimas descargadas
}
