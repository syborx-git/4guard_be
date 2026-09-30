package com.fourguard.wms.application.dto.request.map;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePositionStatusMapRequest {

    @NotNull(message = "El campo 'targetAction' es obligatorio")
    @Schema(description = "Acción operativa: BLOCK, RELEASE u OCCUPY", example = "BLOCK")
    private String targetAction; // 'BLOCK' | 'RELEASE' | 'OCCUPY'

    @Schema(description = "Código o descripción del motivo de bloqueo QM", example = "QM_CONTAMINATION")
    private String reasonCode;

    @Schema(description = "Comentarios u observaciones adicionales del supervisor", example = "Inspección técnica programada")
    private String comment;
}
