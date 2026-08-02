package com.fourguard.wms.application.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fourguard.wms.domain.enums.LicensePlan;
import com.fourguard.wms.shared.jackson.FlexibleOffsetDateTimeDeserializer;
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
    @JsonDeserialize(using = FlexibleOffsetDateTimeDeserializer.class)
    private OffsetDateTime validFrom;

    @NotNull(message = "La fecha de fin de vigencia es obligatoria")
    @JsonDeserialize(using = FlexibleOffsetDateTimeDeserializer.class)
    private OffsetDateTime validUntil;

    private Integer gracePeriodDays;
    private Boolean autoRenewal;

    private Integer maxUsers;
    private Integer maxConcurrentUsers;
    private Integer maxWarehouses;
    private Integer maxHandheldDevices;
    private Integer maxIntegrations;

    private LicenseCapacitiesDto capacities;

    private List<String> enabledModules;

    private String observations;

    public Integer getMaxUsers() {
        if (maxUsers != null) return maxUsers;
        return capacities != null ? capacities.getMaxUsers() : null;
    }

    public Integer getMaxConcurrentUsers() {
        if (maxConcurrentUsers != null) return maxConcurrentUsers;
        return capacities != null ? capacities.getMaxConcurrentUsers() : null;
    }

    public Integer getMaxWarehouses() {
        if (maxWarehouses != null) return maxWarehouses;
        return capacities != null ? capacities.getMaxWarehouses() : null;
    }

    public Integer getMaxHandheldDevices() {
        if (maxHandheldDevices != null) return maxHandheldDevices;
        return capacities != null ? capacities.getMaxHandheldDevices() : null;
    }

    public Integer getMaxIntegrations() {
        if (maxIntegrations != null) return maxIntegrations;
        return capacities != null ? capacities.getMaxIntegrations() : null;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LicenseCapacitiesDto {
        private Integer maxUsers;
        private Integer maxConcurrentUsers;
        private Integer maxWarehouses;
        private Integer maxHandheldDevices;
        private Integer maxIntegrations;
    }
}
