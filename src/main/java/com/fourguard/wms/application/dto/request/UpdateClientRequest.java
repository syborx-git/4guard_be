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

/** Request DTO para actualizar un Cliente Depositante / Owner 3PL existente. */
@Value
@Builder
@Jacksonized
public class UpdateClientRequest {

    @NotNull(message = "El ID del cliente es requerido")
    @Schema(description = "UUID del cliente a actualizar", example = "c73f0907-9fa5-4bdf-87db-2eb5e7683938")
    UUID id;

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

    @Size(max = 300, message = "La dirección no puede superar 300 caracteres")
    @Schema(description = "Dirección Fiscal / Corporativa principal", example = "Av. Ejército Nacional 453, Granada, Miguel Hidalgo, CDMX")
    String address;

    @Size(max = 50, message = "El teléfono no puede superar 50 caracteres")
    @Schema(description = "Teléfono corporativo principal", example = "55 5268 2000")
    String phone;

    @Email(message = "Formato de correo electrónico inválido")
    @Size(max = 150)
    @Schema(description = "Correo electrónico general", example = "contacto@empresa.com")
    String email;

    @Size(max = 255)
    @Schema(description = "Contraseña Portal Autoservicio")
    String webPortalPassword;

    @Schema(description = "Estado del cliente. Si no se envía, conserva el valor actual en BD", example = "ACTIVE")
    String status;

    @Schema(description = "Versión del registro para optimistic locking", example = "2")
    Long version;

    @Valid
    @Builder.Default
    @Schema(description = "Matriz completa de contactos corporativos (reemplaza la colección actual)")
    List<ClientContactDto> contacts = new ArrayList<>();

    @Valid
    @Builder.Default
    @Schema(description = "Lista completa de destinos físicos (reemplaza la colección actual)")
    List<PhysicalDestinationDto> destinations = new ArrayList<>();
}
