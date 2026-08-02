package com.fourguard.wms.application.dto.request;

import com.fourguard.wms.domain.enums.LicensePlan;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RenewLicenseRequest {

    @NotNull(message = "La nueva fecha de vencimiento es obligatoria")
    private OffsetDateTime newValidUntil;

    private LicensePlan newPlan;
    private Boolean autoRenewal;
    private String reason;
}
