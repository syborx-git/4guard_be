package com.fourguard.wms.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;

import java.util.UUID;

/** Request DTO for updating a Client. */
@Value
@Builder
public class UpdateClientRequest {

    @NotNull(message = "El ID del cliente es requerido")
    @Schema(description = "UUID del cliente a actualizar", example = "c73f0907-9fa5-4bdf-87db-2eb5e7683938")
    UUID id;

    @NotNull(message = "El ID de la organización es requerido")
    @Schema(description = "UUID de la organización propietaria", example = "a53f0907-9fa5-4bdf-87db-2eb5e7683935")
    UUID organizationId;

    @Schema(description = "Nombre de la organización (solo referencial, no se persiste en este campo)", example = "4GUARD LOGISTICS CORP")
    String organizationName;

    @NotBlank(message = "El nombre del cliente es requerido")
    @Size(max = 200, message = "El nombre no puede superar 200 caracteres")
    @Schema(description = "Nombre del cliente depositante", example = "Nestle Test")
    String name;

    @Size(max = 50, message = "El ID externo no puede superar 50 caracteres")
    @Schema(description = "Identificador externo del cliente (ERP, SAP, etc.)", example = "NESTLE-TEST-001")
    String externalId;

    @Size(max = 30, message = "El RFC / Tax ID no puede superar 30 caracteres")
    @Schema(description = "RFC o Tax ID del cliente (ej. XAXX010101000 para RFC genérico)", example = "XAXX010101000")
    String taxId;

    @Schema(description = "Estado del cliente. Si no se envía se respeta el valor actual en BD", example = "ACTIVE")
    String status;

    @Schema(description = "Versión del registro para optimistic locking. Si no se envía se respeta el valor actual en BD", example = "1")
    Long version;
}
