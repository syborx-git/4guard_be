package com.fourguard.wms.application.dto.websocket;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BaseWsEventDto<T>(
        UUID eventId,
        String eventType,
        UUID organizationId,
        UUID branchId,
        OffsetDateTime timestamp,
        T data
) {
    public static <T> BaseWsEventDto<T> of(String eventType, UUID organizationId, UUID branchId, T data) {
        return new BaseWsEventDto<>(
                UUID.randomUUID(),
                eventType,
                organizationId,
                branchId,
                OffsetDateTime.now(),
                data
        );
    }
}
