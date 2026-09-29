package com.fourguard.wms.application.dto.request.quality;

import com.fourguard.wms.domain.enums.DefectCategory;
import com.fourguard.wms.domain.enums.DetectionStage;
import com.fourguard.wms.domain.enums.QualitySeverity;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CreateQualityBlockRequest {

    @NotNull(message = "El item_id (tarima) es obligatorio")
    private UUID itemId;

    @NotNull(message = "La etapa de detección es obligatoria")
    private DetectionStage stage;

    @NotNull(message = "La categoría de defecto es obligatoria")
    private DefectCategory defectCategory;

    @NotEmpty(message = "Debe especificar al menos un criterio de defecto")
    private List<String> defectCriteria;

    @NotNull(message = "La severidad es obligatoria")
    private QualitySeverity severity;

    @NotNull(message = "La cantidad retenida es obligatoria")
    @DecimalMin(value = "0.001", message = "La cantidad debe ser mayor a 0")
    private BigDecimal quantity;

    @NotBlank(message = "Las notas/observaciones son obligatorias (mínimo 5 caracteres)")
    @Size(min = 5, max = 1000)
    private String notes;

    private UUID targetLocationId;
    private List<EvidenceFileDto> evidenceFiles;
}
