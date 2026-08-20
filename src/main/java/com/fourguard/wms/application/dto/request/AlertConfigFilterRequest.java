package com.fourguard.wms.application.dto.request;

import com.fourguard.wms.domain.enums.AlertCategory;
import com.fourguard.wms.domain.enums.AlertEvent;
import com.fourguard.wms.domain.enums.AlertPriority;
import com.fourguard.wms.domain.enums.AlertStatus;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertConfigFilterRequest {

    private UUID organizationId;
    private AlertCategory category;
    private AlertEvent event;
    private AlertPriority priority;
    private AlertStatus status;
    private String search;
}
