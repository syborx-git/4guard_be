package com.fourguard.wms.application.dto.websocket;

import lombok.Builder;
import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
public record RampEventDto(
        String eventType,           // RAMP_ASSIGNED, RAMP_RELEASED, RAMP_BLOCKED
        UUID rampId,
        String rampCode,
        Integer rampNumber,
        String status,              // AVAILABLE, OCCUPIED, BLOCKED, MAINTENANCE
        String operationType,       // INBOUND, OUTBOUND, NONE
        String currentFolio,        // Folio de recepción o salida asignada
        String carrierName,
        String driverName,
        String forkliftOperatorName,
        OffsetDateTime occupiedAt,
        OffsetDateTime releasedAt
) {}
