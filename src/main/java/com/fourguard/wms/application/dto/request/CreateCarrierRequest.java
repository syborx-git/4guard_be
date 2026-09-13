package com.fourguard.wms.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

/** Request DTO for creating a Carrier. */
@Value
@Builder
public class CreateCarrierRequest {

    @NotNull(message = "El ID de la organización es requerido")
    @Schema(description = "UUID de la organización propietaria", example = "a53f0907-9fa5-4bdf-87db-2eb5e7683935")
    UUID organizationId;

    @Schema(description = "Nombre de la organización (solo referencial)", example = "4GUARD LOGISTICS CORP")
    String organizationName;

    @NotBlank(message = "El nombre del transportista es requerido")
    @Size(min = 2, max = 200, message = "El nombre debe tener entre 2 y 200 caracteres")
    @Schema(description = "Nombre o razón social del transportista", example = "Transportes del Noreste S.A. de C.V.")
    String name;

    @NotBlank(message = "El nombre comercial es requerido")
    @Size(max = 200, message = "El nombre comercial no puede superar 200 caracteres")
    @Schema(description = "Nombre comercial", example = "TransNoreste")
    String tradeName;

    @Size(max = 30, message = "El tax ID / RFC no puede superar 30 caracteres")
    @Pattern(regexp = "^$|^[A-Z0-9&\\-\\.\\s]*$", message = "RFC con formato inválido")
    @Schema(description = "RFC o Tax ID del transportista (opcional)", example = "TN0890314AB2")
    String taxId;

    @Schema(description = "Tipo de transportista (EXTERNAL, CLIENT_TRANSPORT, OWN_TRANSPORT, THIRD_PARTY_3PL, PARCEL)", example = "EXTERNAL")
    String carrierType;

    @Size(max = 150, message = "El nombre del contacto no puede superar 150 caracteres")
    @Schema(description = "Nombre del representante o contacto (opcional)", example = "Roberto Garza Hernández")
    String contactName;

    @Size(max = 20, message = "El teléfono no puede superar 20 caracteres")
    @Pattern(regexp = "^$|^\\+?[0-9\\s\\-()]{7,20}$", message = "Formato de teléfono inválido")
    @Schema(description = "Teléfono de contacto (opcional)", example = "8181234567")
    String contactPhone;

    @Email(message = "Debe proporcionar un correo electrónico válido")
    @Size(max = 255, message = "El correo no puede superar 255 caracteres")
    @Schema(description = "Correo electrónico de contacto (opcional)", example = "rgarza@transnoreste.com.mx")
    String contactEmail;

    @Schema(description = "Tipo de servicio (FTL, LTL, PARCEL, INTERMODAL, LAST_MILE, DEDICATED)", example = "FTL")
    String serviceType;

    @Schema(description = "Número de registro o permiso oficial (ej: permiso SCT)", example = "SCT-NL-00234-2022")
    String permitNumber;

    @Schema(description = "Regiones, estados o cobertura geográfica que cubre el transportista", example = "Noreste, Centro y Bajío (NL, CDMX, QRO, GTO)")
    String geographicCoverage;

    @Schema(description = "Observaciones generales de contratación o estado", example = "Transportista preferencial para rutas de alto volumen. Contrato vigente hasta 2027.")
    String notes;

    @Schema(description = "Lista de capacidades de tipos de unidades (Caja seca, Plataforma, Tractocamión, etc.)")
    List<String> vehicleTypes;

    @Schema(description = "Lista de UUIDs de clientes preferentes a asociar con el transportista")
    List<UUID> preferredClientIds;

    @Schema(description = "Estado del transportista. Por defecto ACTIVE si no se envía", example = "ACTIVE")
    String status;

    @Schema(description = "Versión del registro para optimistic locking. Por defecto 1 si no se envía", example = "1")
    Long version;
}
