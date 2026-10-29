package com.fourguard.wms.application.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangePasswordRequest {

    @NotBlank(message = "La nueva contraseña o PIN es requerida")
    @Size(min = 4, message = "La contraseña o PIN debe tener al menos 4 caracteres de longitud")
    private String newPassword;
}
