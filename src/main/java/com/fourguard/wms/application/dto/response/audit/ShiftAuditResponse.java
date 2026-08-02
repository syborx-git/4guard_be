package com.fourguard.wms.application.dto.response.audit;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class ShiftAuditResponse {
    private final UUID logId;
    private final String action;
    private final String username;
    private final OffsetDateTime createdAt;
    private final List<AuditDetailResponse> details;

    @Value
    @Builder
    public static class AuditDetailResponse {
        String fieldName;
        String oldValue;
        String newValue;
    }
}
