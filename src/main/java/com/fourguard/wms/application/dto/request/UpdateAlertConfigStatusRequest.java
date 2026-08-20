package com.fourguard.wms.application.dto.request;

import com.fourguard.wms.domain.enums.AlertStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateAlertConfigStatusRequest {

    @NotNull(message = "El estado de la regla es obligatorio")
    private AlertStatus status;
}
