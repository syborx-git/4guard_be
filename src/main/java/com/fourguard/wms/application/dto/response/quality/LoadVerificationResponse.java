package com.fourguard.wms.application.dto.response.quality;

import com.fourguard.wms.application.dto.request.quality.EvidenceFileDto;
import com.fourguard.wms.application.dto.request.quality.VerificationCriterionDto;
import com.fourguard.wms.application.dto.request.quality.VerificationSignaturesDto;
import com.fourguard.wms.domain.enums.LoadVerificationStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class LoadVerificationResponse {
    private UUID id;
    private String folio;
    private String controlNumber;
    private String revisionNumber;
    private String processName;
    private String ownerDepartment;
    private UUID outboundId;
    private UUID receptionId;
    private String remisionNumber;
    private String productDescription;
    private String clientName;
    private LocalDate date;
    private LocalTime time;
    private String ramp;
    private LoadVerificationStatus status;
    private List<VerificationCriterionDto> productCriteria;
    private List<VerificationCriterionDto> transportCriteria;
    private VerificationSignaturesDto signatures;
    private String generalObservations;
    private List<EvidenceFileDto> evidencePhotos;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
