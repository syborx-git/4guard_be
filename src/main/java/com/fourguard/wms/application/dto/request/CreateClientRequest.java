package com.fourguard.wms.application.dto.request;

import com.fourguard.wms.application.dto.common.ClientContactDto;
import com.fourguard.wms.application.dto.common.PhysicalDestinationDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Request DTO para crear un nuevo Cliente Depositante / Owner 3PL. */
@Value
@Builder
@Jacksonized
public class CreateClientRequest {

    @NotNull(message = "El ID de la organización es requerido")
    @Schema(description = "UUID de la organización propietaria", example = "a53f0907-9fa5-4bdf-87db-2eb5e7683935")
    UUID organizationId;

    @Schema(description = "Nombre de la organización (solo referencial)", example = "4GUARD LOGISTICS CORP")
    String organizationName;

    @NotBlank(message = "La razón social del cliente es requerida")
    @Size(max = 200, message = "La razón social no puede superar 200 caracteres")
    @Schema(description = "Razón Social / Nombre del cliente depositante", example = "NESTLE MEXICO S.A. DE C.V.")
    String name;

    @Size(max = 50, message = "El código de cliente / RFC no puede superar 50 caracteres")
    @Schema(description = "Código ERP / RFC fiscal del cliente", example = "NME850101K99")
    String externalId;

    @Size(max = 30, message = "El Tax ID / RFC SAT no puede superar 30 caracteres")
    @Schema(description = "RFC o Tax ID para validación SAT", example = "XAXX010101000")
    String taxId;

    @NotBlank(message = "La dirección fiscal es requerida")
    @Size(max = 300, message = "La dirección no puede superar 300 caracteres")
    @Schema(description = "Dirección Fiscal / Corporativa principal", example = "Av. Ejército Nacional 453, Granada, Miguel Hidalgo, CDMX")
    String address;

    @NotBlank(message = "El teléfono corporativo es requerido")
    @Size(max = 50, message = "El teléfono no puede superar 50 caracteres")
    @Schema(description = "Teléfono corporativo principal", example = "55 5268 2000")
    String phone;

    @Email(message = "Formato de correo electrónico inválido")
    @Size(max = 150)
    @Schema(description = "Correo electrónico general", example = "contacto@empresa.com")
    String email;

    @Size(max = 255)
    @Schema(description = "Contraseña Portal Autoservicio", example = "EmpresaPortal#2026")
    String webPortalPassword;

    @Schema(description = "Estado del cliente. Por defecto ACTIVE", example = "ACTIVE")
    String status;

    @Schema(description = "Versión del registro. Por defecto 1", example = "1")
    Long version;

    @Valid
    @Builder.Default
    @Schema(description = "Matriz de contactos corporativos")
    List<ClientContactDto> contacts = new ArrayList<>();

    @Valid
    @Builder.Default
    @Schema(description = "Direcciones físicas de destino (bodegas/plantas)")
    List<PhysicalDestinationDto> destinations = new ArrayList<>();
}
