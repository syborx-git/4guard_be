package com.fourguard.wms.application.dto.response.reception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyPalletResponse {
    private boolean valid;
    private String status; // "VERIFIED", "ALREADY_VERIFIED", "DISCREPANCY"
    private UUID palletId;
    private String palletCode;
    private String lotNumber;
    private String skuCode;
    private String productName;
    private BigDecimal pieces;
    private LocalDate expirationDate;
    private int verifiedCount;
    private int totalExpected;
    private int remainingCount;
    private String message;
}
