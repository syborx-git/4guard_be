package com.fourguard.wms.application.dto.request;

import com.fourguard.wms.domain.enums.CurrencyStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCurrencyStatusRequest {

    @NotNull(message = "El estatus es obligatorio (ACTIVE, INACTIVE)")
    private CurrencyStatus status;
}
