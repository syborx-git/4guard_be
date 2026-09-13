package com.fourguard.wms.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Value;

/** Sub-DTO for supplier contact data (nested in Create/UpdateSupplierRequest). */
@Value
@Builder
public class SupplierContactRequest {

    @NotBlank(message = "El nombre completo del contacto es requerido")
    @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
    @Schema(description = "Nombre completo del contacto principal", example = "Carlos Eduardo Mendoza")
    String fullName;

    @Size(max = 100, message = "El cargo no puede superar 100 caracteres")
    @Schema(description = "Cargo o puesto del contacto", example = "Gerente de Cuentas Clave")
    String jobTitle;

    @NotBlank(message = "El correo electrónico del contacto es requerido")
    @Email(message = "Correo electrónico inválido")
    @Size(max = 150, message = "El correo no puede superar 150 caracteres")
    @Schema(description = "Correo electrónico del contacto", example = "cmendoza@empaquesnorte.com.mx")
    String email;

    @Size(max = 25, message = "El teléfono no puede superar 25 caracteres")
    @Schema(description = "Teléfono principal del contacto (opcional)", example = "8183456789")
    String phone;

    @Size(max = 25, message = "El teléfono alterno no puede superar 25 caracteres")
    @Schema(description = "Teléfono alternativo del contacto", example = "8181239900")
    String altPhone;
}
