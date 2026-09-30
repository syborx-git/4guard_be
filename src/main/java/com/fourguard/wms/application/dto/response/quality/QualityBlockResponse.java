package com.fourguard.wms.application.dto.response.quality;

import com.fourguard.wms.application.dto.request.quality.EvidenceFileDto;
import com.fourguard.wms.domain.enums.DefectCategory;
import com.fourguard.wms.domain.enums.DetectionStage;
import com.fourguard.wms.domain.enums.QualitySeverity;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class QualityBlockResponse {
    private UUID id;
    private String folio;
    private UUID itemId;
    private String sscc;
    private String sku;
    private String skuDescription;
    private String clientName;
    private String batchNumber;
    private BigDecimal quantity;
    private String unitOfMeasure;
    private String locationCode;
    private DetectionStage stage;
    private DefectCategory defectCategory;
    private List<String> defectCriteria;
    private QualitySeverity severity;
    private String status; // BLOCKED, UNDER_INSPECTION, RELEASED
    private String reportedByName;
    private OffsetDateTime reportedAt;
    private String notes;
    private List<EvidenceFileDto> evidenceFiles;
}
