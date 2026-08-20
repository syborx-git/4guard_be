package com.fourguard.wms.application.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO de Contacto Corporativo del Cliente.
 * Usado en requests (CreateClientRequest / UpdateClientRequest) y en ClientResponse.
 */
@Getter
@Builder
@Jacksonized
@Schema(description = "Contacto corporativo de un cliente depositante")
public class ClientContactDto {

    @Schema(description = "UUID del contacto (null en creación)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private final UUID id;

    @NotBlank(message = "El nombre del contacto es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
    @Schema(description = "Nombre completo del contacto", example = "Ing. Carlos Fuentes")
    private final String name;

    @NotBlank(message = "El departamento es obligatorio")
    @Size(max = 100, message = "El departamento no puede superar 100 caracteres")
    @Schema(description = "Departamento o área del contacto", example = "Logística y Abasto")
    private final String department;

    @NotBlank(message = "El teléfono directo es obligatorio")
    @Size(max = 50, message = "El teléfono no puede superar 50 caracteres")
    @Schema(description = "Teléfono directo del contacto", example = "55 1234 5678")
    private final String phone;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Formato de correo electrónico inválido")
    @Size(max = 150)
    @Schema(description = "Correo electrónico corporativo", example = "cfuentes@empresa.com")
    private final String email;

    @Schema(description = "Indica si es el contacto principal del cliente", example = "true")
    private final Boolean isPrimary;

    @Schema(description = "Fecha de creación (solo lectura en response)")
    private final OffsetDateTime createdAt;

    @Schema(description = "Fecha de última actualización (solo lectura en response)")
    private final OffsetDateTime updatedAt;
}
