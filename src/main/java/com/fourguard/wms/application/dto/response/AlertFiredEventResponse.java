package com.fourguard.wms.application.dto.response;

import com.fourguard.wms.domain.enums.FiredEventStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertFiredEventResponse {

    private UUID id;
    private UUID alertConfigurationId;
    private UUID organizationId;
    private UUID branchId;
    private OffsetDateTime triggeredAt;
    private String entityReference;
    private BigDecimal evaluatedValue;
    private FiredEventStatus status;
    private String acknowledgedBy;
    private OffsetDateTime acknowledgedAt;
}
