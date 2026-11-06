package com.fourguard.wms.application.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogoutRequest {

    @NotBlank(message = "El Refresh Token es requerido para cerrar sesión")
    private String refreshToken;

    /**
     * Motivo de cierre de sesión: "MANUAL", "TIMEOUT", "REVOKED", etc.
     */
    private String reason;
}
