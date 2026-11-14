package com.fourguard.wms.application.dto.websocket;

import lombok.Builder;
import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
public record ForkliftMissionEventDto(
        UUID missionId,
        String folio,
        String missionType,         // INBOUND_DISCHARGE, OUTBOUND_PICKING, INTERNAL_TRANSFER, PUTAWAY
        String status,              // ASSIGNED, IN_PROGRESS, COMPLETED, PAUSED
        String priority,            // NORMAL, URGENT, CRITICAL
        UUID organizationId,
        UUID branchId,
        UUID forkliftOperatorId,
        String forkliftOperatorCode,
        String forkliftOperatorName,
        String originLocation,
        String destinationLocation,
        String rampCode,
        Integer totalPallets,
        Integer totalPieces,
        String skuCode,
        String skuName,
        OffsetDateTime timestamp
) {}
