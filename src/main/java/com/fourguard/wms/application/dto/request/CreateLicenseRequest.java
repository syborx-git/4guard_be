package com.fourguard.wms.application.dto.request;

import com.fourguard.wms.domain.enums.LicensePlan;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateLicenseRequest {

    @NotNull(message = "El ID de la organización es obligatorio")
    private UUID organizationId;

    @NotNull(message = "El nombre de la licencia es obligatorio")
    @Size(max = 150, message = "El nombre no puede exceder 150 caracteres")
    private String licenseName;

    @NotNull(message = "El plan es obligatorio")
    private LicensePlan plan;

    private String description;

    @NotNull(message = "La fecha de inicio de vigencia es obligatoria")
    private OffsetDateTime validFrom;

    @NotNull(message = "La fecha de fin de vigencia es obligatoria")
    private OffsetDateTime validUntil;

    private Integer gracePeriodDays;
    private Boolean autoRenewal;

    private Integer maxUsers;
    private Integer maxConcurrentUsers;
    private Integer maxWarehouses;
    private Integer maxHandheldDevices;
    private Integer maxIntegrations;

    private List<String> enabledModules;

    private String observations;
}
