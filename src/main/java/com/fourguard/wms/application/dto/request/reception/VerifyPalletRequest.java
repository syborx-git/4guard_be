package com.fourguard.wms.application.dto.request.reception;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyPalletRequest {
    @NotBlank(message = "El código de tarima (UA) es obligatorio")
    private String palletCode;
    private UUID forkliftOperatorId;
}
