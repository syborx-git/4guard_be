package com.fourguard.wms.application.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO para la activación e inicialización operativa de una nave/sección de almacén.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitializeWarehouseSectionRequest {

    @NotBlank(message = "La categoría operativa es requerida")
    @Size(max = 100, message = "La categoría no puede superar 100 caracteres")
    private String category;

    @NotNull(message = "El número de posiciones fijas es requerido")
    @Min(value = 1, message = "Debe registrar al menos 1 posición fija")
    private Integer posFijas;

    @NotNull(message = "La capacidad de tarimas es requerida")
    @Min(value = 1, message = "La capacidad debe ser mayor a 0")
    private Integer capacidadTarimas;

    @NotBlank(message = "El factor de estiba es requerido")
    @Size(max = 50, message = "El factor de estiba no puede superar 50 caracteres")
    private String factorEstiba;

    private String notes;

    @Builder.Default
    private Boolean generateLocations = true;

    private List<UUID> authorizedSkuIds;
}
