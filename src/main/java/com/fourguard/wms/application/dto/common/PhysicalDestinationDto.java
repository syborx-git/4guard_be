package com.fourguard.wms.application.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO de Dirección Física de Destino (Bodega / Planta / Ship-to Location).
 * Usado en requests (CreateClientRequest / UpdateClientRequest),
 * en ClientResponse y en los endpoints directos de /clients/{id}/destinations.
 */
@Getter
@Builder
@Jacksonized
@Schema(description = "Dirección física de destino (bodega, planta, CEDIS) de un cliente 3PL")
public class PhysicalDestinationDto {

    @Schema(description = "UUID del destino (null en creación)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private final UUID id;

    @NotBlank(message = "El código de destino es obligatorio")
    @Size(max = 50, message = "El código no puede superar 50 caracteres")
    @Schema(description = "Código correlativo del destino", example = "DEST-TOL-01")
    private final String destinationCode;

    @NotBlank(message = "El nombre de la planta / bodega es obligatorio")
    @Size(max = 200, message = "El nombre no puede superar 200 caracteres")
    @Schema(description = "Nombre identificador de la planta o bodega", example = "Planta Toluca (Café y Cacao)")
    private final String plantName;

    @NotBlank(message = "La dirección completa de entrega es obligatoria")
    @Size(max = 500, message = "La dirección no puede superar 500 caracteres")
    @Schema(description = "Dirección física completa de entrega", example = "Km 62.5 Carretera México-Toluca, Zona Industrial Toluca")
    private final String fullAddress;

    @NotBlank(message = "La persona de contacto en sitio es obligatoria")
    @Size(max = 150, message = "El nombre del contacto no puede superar 150 caracteres")
    @Schema(description = "Responsable o contacto en sitio", example = "Ing. Fernando Ruiz")
    private final String contactPerson;

    @NotBlank(message = "El teléfono de la planta es obligatorio")
    @Size(max = 50, message = "El teléfono no puede superar 50 caracteres")
    @Schema(description = "Teléfono directo de la planta o bodega", example = "722 279 1000")
    private final String phone;

    @Schema(description = "Estado operativo del destino", example = "ACTIVO", allowableValues = {"ACTIVO", "INACTIVO"})
    private final String status;

    @Schema(description = "Indicaciones especiales de acceso o descarga")
    private final String notes;

    @Schema(description = "Versión del registro para optimistic locking")
    private final Long version;

    @Schema(description = "Fecha de creación (solo lectura en response)")
    private final OffsetDateTime createdAt;

    @Schema(description = "Fecha de última actualización (solo lectura en response)")
    private final OffsetDateTime updatedAt;
}
