package com.fourguard.wms.application.dto.response.transfer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Summary DTO for transfer list views.
 * ADR-011, ADR-017, ADR-020, ADR-022.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferSummaryResponse {

    private UUID id;
    private String folio;
    private String status;
    private String priority;
    private UUID originLocationId;
    private String originLocationCode;
    private UUID destinationLocationId;
    private String destinationLocationCode;
    private UUID forkliftOperatorId;
    private String forkliftOperatorName;
    private String reasonCode;
    private String reasonLabel;
    private Integer totalPallets;
    private Double totalPieces;
    private Integer distinctSkus;
    private OffsetDateTime startedAt;
    private OffsetDateTime completedAt;
    private OffsetDateTime pausedAt;
    private String pauseReason;
    private OffsetDateTime createdAt;
    private String createdBy;
    private UUID shiftSessionId;
}
