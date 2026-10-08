package com.fourguard.wms.application.dto.request.map;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePositionMapRequest {

    @NotNull(message = "El ID de la sección/almacén es requerido")
    private UUID sectionId;

    private String code;

    @Builder.Default
    private String category = "FIXED_STORAGE"; // FIXED_STORAGE, TEMPORARY_BUFFER, PRELOAD_STAGING

    @Builder.Default
    private Integer capacityTarimas = 22;

    private String aisle;
    private String rack;
    private Integer level;
    private String notes;
}
