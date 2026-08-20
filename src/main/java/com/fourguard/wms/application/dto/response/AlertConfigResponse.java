package com.fourguard.wms.application.dto.response;

import com.fourguard.wms.domain.enums.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertConfigResponse {

    private UUID id;
    private UUID organizationId;
    private String name;
    private AlertCategory category;
    private AlertEvent event;
    private AlertPriority priority;
    private AlertStatus status;
    private List<String> channels;
    private List<String> recipients;
    private AlertCondition condition;
    private BigDecimal value;
    private AlertUnit unit;
    private AlertRecurrence recurrence;
    private AlertEscalation escalation;
    private String messageTemplate;
    private String description;
    private Boolean isDeleted;
    private Long version;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
