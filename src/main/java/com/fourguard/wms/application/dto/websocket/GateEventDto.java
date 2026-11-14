package com.fourguard.wms.application.dto.websocket;

import lombok.Builder;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Builder
public record GateEventDto(
        String eventType,           // DRIVER_PRE_CHECKIN_SUBMITTED, GUARD_CHECKIN_COMPLETED, GUARD_CHECKOUT_COMPLETED
        UUID preCheckinId,
        String folio,
        String token,
        String operationType,       // CARGA, DESCARGA, REINGRESO
        String status,              // SUBMITTED, COMPLETED, COMPLETED_EXIT
        String driverName,
        String driverPhone,
        String tractorPlates,
        String boxPlates,
        String carrierName,
        String clientName,
        String rampCode,
        Integer rampNumber,
        List<String> sealNumbers,
        String observations,
        OffsetDateTime timestamp
) {}
