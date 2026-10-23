package com.fourguard.wms.application.dto.response.history;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
public class RemissionHistoryItemResponse {
    private final UUID id;
    private final String entityType; // RECEPTION | OUTBOUND | TRANSFER
    private final String folio;
    private final String remisionNumber;
    private final UUID clientId;
    private final String clientName;
    private final UUID branchId;
    private final String branchName;
    private final String status;
    private final UUID shiftSessionId;
    private final String shiftName;
    private final String operator;
    private final Integer pallets;
    private final BigDecimal pieces;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime completedAt;
    private final OffsetDateTime cancelledAt;
    private final String cancellationReason;
}
