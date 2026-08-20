package com.fourguard.wms.application.dto.response.audit;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Response DTO representing global user activity audit logs. */
@Getter
@Builder
public class UserActivityAuditResponse {
    private final UUID logId;
    private final UUID userId;
    private final String username;
    private final String action;
    private final String entityType;
    private final UUID entityId;
    private final String ipAddress;
    private final String userAgent;
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
