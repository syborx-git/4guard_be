package com.fourguard.wms.application.dto.response.reception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Summary DTO for reception master directory listing.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceptionSummaryResponse {

    private UUID id;
    private String folio;
    private String status;

    // Check-in summary
    private String docNumber;
    private LocalDate docDate;
    private LocalTime receptionTime;
    private UUID clientId;
    private String clientName;
    private UUID carrierId;
    private String carrierName;
    private UUID rampId;
    private String rampName;
    private String driverName;
    private String tractorPlates;
    private String boxPlates;

    // Product summary
    private UUID skuId;
    private String skuCode;
    private String productName;
    private String lotNumber;
    private Integer totalPallets;
    private Double totalPieces;

    // Audit timestamps
    private OffsetDateTime createdAt;
    private OffsetDateTime completedAt;
    private OffsetDateTime cancelledAt;
    private String capturedBy;
}
