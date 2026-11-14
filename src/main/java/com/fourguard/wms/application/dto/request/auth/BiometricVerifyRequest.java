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
public class BiometricVerifyRequest {

    @NotBlank(message = "El identificador de credencial biométrica es requerido")
    private String credentialId;

    private String clientDataJSON;
    private String authenticatorData;
    private String signature;
    private String userHandle;
}

