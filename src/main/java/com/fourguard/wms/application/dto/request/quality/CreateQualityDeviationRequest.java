package com.fourguard.wms.application.dto.request.quality;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateQualityDeviationRequest {

    @NotBlank(message = "El número de remisión es obligatorio")
    private String remisionNumber;

    @NotBlank(message = "El SKU del material es obligatorio")
    private String skuId;

    private String skuDescription;

    @NotBlank(message = "El código UA/SSCC es obligatorio")
    private String uaCode;

    @NotBlank(message = "El tipo de material es obligatorio")
    private String materialType; // PRODUCTO_TERMINADO, EMBALAJES, CAFE_VERDE, OTRO

    @NotNull(message = "La fecha de la desviación es obligatoria")
    private LocalDate deviationDate;

    private LocalTime deviationTime;

    private String responsibleCollaborator;

    private String bayLocationCode;

    @NotNull(message = "El conteo de unidades dañadas es obligatorio")
    @Min(value = 0, message = "Las unidades no pueden ser negativas")
    private Integer damagedUnits;

    @NotNull(message = "El costo financiero es obligatorio")
    @DecimalMin(value = "0.00", message = "El costo no puede ser negativo")
    private BigDecimal materialCost;

    private String currency; // Default: MXN

    @NotBlank(message = "La desviación en condiciones es obligatoria")
    private String conditionDeviation;

    @NotBlank(message = "El motivo de causa raíz es obligatorio")
    private String rootCauseMotive;

    @NotBlank(message = "El área de origen es obligatoria")
    private String originArea;

    @NotBlank(message = "La acción realizada es obligatoria")
    private String actionTaken;

    private String observations;

    private List<String> evidencePhotoUrls;
}
