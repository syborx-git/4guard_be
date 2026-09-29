package com.fourguard.wms.application.dto.response.quality;

import com.fourguard.wms.application.dto.request.quality.EvidenceFileDto;
import com.fourguard.wms.domain.enums.DetectionStage;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class QualityClaimResponse {
    private UUID id;
    private String folio;
    private LocalDate date;
    private LocalTime time;
    private DetectionStage stage;
    private String sku;
    private String productDescription;
    private String clientName;
    private String batchNumber;
    private String remisionNumber;
    private String defectType;
    private String defectCustomType;
    private BigDecimal damagedQty;
    private BigDecimal lostQty;
    private BigDecimal associatedCost;
    private String currency;
    private String authorizedByName;
    private String authorizedByPosition;
    private String observations;
    private List<EvidenceFileDto> evidenceFiles;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
