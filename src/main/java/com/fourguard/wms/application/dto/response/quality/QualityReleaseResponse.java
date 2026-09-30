package com.fourguard.wms.application.dto.response.quality;

import com.fourguard.wms.application.dto.request.quality.EvidenceFileDto;
import com.fourguard.wms.domain.enums.ReleaseAuthorizerType;
import com.fourguard.wms.domain.enums.ReleaseDestination;
import com.fourguard.wms.domain.enums.ReleaseSupportType;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class QualityReleaseResponse {
    private UUID id;
    private String folio;
    private UUID blockId;
    private String blockFolio;
    private String sku;
    private String description;
    private String batchNumber;
    private String clientName;
    private BigDecimal quantity;
    private String unitOfMeasure;
    private ReleaseAuthorizerType authorizerType;
    private ReleaseSupportType supportType;
    private String supportCustomType;
    private String supportSubject;
    private String supportFileName;
    private String authorizedByName;
    private String authorizedByPosition;
    private ReleaseDestination destination;
    private String decisionNotes;
    private String releasedByUserName;
    private OffsetDateTime releasedAt;
    private List<EvidenceFileDto> evidenceFiles;
}
