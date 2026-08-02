package com.fourguard.wms.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SuspendLicenseRequest {

    @NotBlank(message = "El motivo de suspensión es obligatorio")
    private String reason;
}
