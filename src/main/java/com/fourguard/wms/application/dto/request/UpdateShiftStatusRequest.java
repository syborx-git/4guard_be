package com.fourguard.wms.application.dto.request;

import com.fourguard.wms.domain.enums.ShiftStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateShiftStatusRequest {

    @NotNull(message = "El nuevo estado del turno es requerido")
    private ShiftStatus status;
}
