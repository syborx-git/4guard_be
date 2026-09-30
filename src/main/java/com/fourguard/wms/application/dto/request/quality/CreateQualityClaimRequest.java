package com.fourguard.wms.application.dto.request.quality;

import com.fourguard.wms.domain.enums.DetectionStage;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CreateQualityClaimRequest {

    private UUID itemId;

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate date;

    @NotNull(message = "La hora es obligatoria")
    private LocalTime time;

    @NotNull(message = "La etapa es obligatoria")
    private DetectionStage stage;

    @NotBlank(message = "El SKU es obligatorio")
    private String sku;

    @NotBlank(message = "La descripción del producto es obligatoria")
    private String productDescription;

    @NotBlank(message = "El cliente es obligatorio")
    private String clientName;

    private String batchNumber;
    private String remisionNumber;

    @NotBlank(message = "El tipo de defecto es obligatorio")
    private String defectType;

    private String defectCustomType;

    @Min(value = 0, message = "Las piezas dañadas no pueden ser negativas")
    private BigDecimal damagedQty;

    @Min(value = 0, message = "Las piezas perdidas no pueden ser negativas")
    private BigDecimal lostQty;

    @DecimalMin(value = "0.0", message = "El costo asociado no puede ser negativo")
    private BigDecimal associatedCost;

    @NotBlank(message = "La moneda es obligatoria (MXN / USD)")
    private String currency;

    @NotBlank(message = "El nombre de quien autoriza es obligatorio")
    private String authorizedByName;

    private String authorizedByPosition;
    private String observations;
    private List<EvidenceFileDto> evidenceFiles;
}
