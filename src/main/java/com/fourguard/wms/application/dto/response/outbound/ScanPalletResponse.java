package com.fourguard.wms.application.dto.response.outbound;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Response DTO for instant RF scanner / barcode lookup of a pallet (UA/SSCC).
 * Provides full product, batch, location, and FEFO / Pablo life cycle metadata.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScanPalletResponse {

    private UUID itemId;
    private String palletCode;
    private String externalUa;
    private UUID skuId;
    private String skuCode;
    private String productName;
    private String category;
    private UUID clientId;
    private String clientName;
    private String lotNumber;
    private String inboundRemisionNo;
    private LocalDate manufacturingDate;
    private LocalDate expirationDate;
    private Long daysRemaining;
    private String pabloStatus;
    private String pabloLabel;
    private Boolean isSuggestedFefo;
    private Double pieces;
    private String palletTypeId;
    private String palletTypeLabel;
    private String locationCode;
    private String state;
}
