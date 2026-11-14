package com.fourguard.wms.application.dto.websocket;

import lombok.Builder;
import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
public record QualityBlockEventDto(
        UUID eventId,
        String eventType,           // QM_BLOCK_ALERT, QM_RELEASE_AUTHORIZED
        String severity,            // CRITICAL, HIGH, MEDIUM, LOW, INFO
        String sscc,
        String palletFolio,
        String sku,
        String productName,
        String locationCode,
        String reason,
        String recommendedAction,
        String requiredInstruction,
        String reportedBy,
        OffsetDateTime timestamp
) {}
