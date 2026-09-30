package com.fourguard.wms.application.dto.request.quality;

import com.fourguard.wms.domain.enums.LoadVerificationStatus;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SaveLoadVerificationRequest {

    private UUID id;
    private UUID outboundId;
    private UUID receptionId;

    @NotBlank(message = "El número de remisión es obligatorio")
    private String remisionNumber;

    @NotBlank(message = "La descripción del producto es obligatoria")
    private String productDescription;

    @NotBlank(message = "El nombre del cliente es obligatorio")
    private String clientName;

    @NotNull(message = "La fecha de verificación es obligatoria")
    private LocalDate date;

    @NotNull(message = "La hora de verificación es obligatoria")
    private LocalTime time;

    @NotBlank(message = "La rampa es obligatoria")
    private String ramp;

    @NotNull(message = "El estatus de verificación es obligatorio")
    private LoadVerificationStatus status;

    @NotEmpty(message = "Los criterios de producto son obligatorios")
    private List<VerificationCriterionDto> productCriteria;

    @NotEmpty(message = "Los criterios de transporte son obligatorios")
    private List<VerificationCriterionDto> transportCriteria;

    @NotNull(message = "El bloque de firmas es obligatorio")
    private VerificationSignaturesDto signatures;

    private String generalObservations;
    private List<EvidenceFileDto> evidencePhotos;
}
