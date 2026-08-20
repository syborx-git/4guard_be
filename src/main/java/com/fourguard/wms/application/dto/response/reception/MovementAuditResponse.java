package com.fourguard.wms.application.dto.response.reception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for Movement Audit entries and timeline visualization.
 * Homologated with Frontend `MovementAuditEntry`.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovementAuditResponse {

    private String id;
    private String action;
    private String actionLabel;
    private String username;
    private String timestamp;
    private String reason;
    private String authorizedBy;
    private String observations;
    private List<MovementAuditDetailResponse> details;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MovementAuditDetailResponse {
        private String fieldName;
        private String oldValue;
        private String newValue;
    }
}
