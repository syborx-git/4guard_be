package com.fourguard.wms.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.UUID;

/** Request DTO for updating a Product SKU. */
@Value
@Builder
public class UpdateProductSkuRequest {

    @NotNull(message = "El ID del SKU es requerido")
    UUID id;

    @NotNull(message = "El ID del cliente es requerido")
    UUID clientId;

    @NotBlank(message = "El código SKU es requerido")
    @Size(max = 50, message = "El código SKU no puede superar 50 caracteres")
    String code;

    @NotBlank(message = "El nombre del SKU es requerido")
    @Size(max = 200, message = "El nombre no puede superar 200 caracteres")
    String name;

    String description;

    BigDecimal weight;

    @NotBlank(message = "La unidad de medida es requerida")
    @Size(max = 20, message = "La unidad no puede superar 20 caracteres")
    String unit;

    String status;

    Boolean isDeleted;
}
