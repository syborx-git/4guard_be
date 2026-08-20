package com.fourguard.wms.application.dto.response.outbound;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for available inventory batches ordered by FIFO/FEFO.
 * Homologated with Frontend `InventoryBatch`.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryBatchResponse {

    private String remisionNo;
    private UUID clientId;
    private String clientName;
    private UUID skuId;
    private String skuCode;
    private String productName;
    private String lotNumber;
    private LocalDate manufacturingDate;
    private LocalDate expirationDate;
    private Integer availablePallets;
    private Double totalPieces;
    private String locationCode;
    private Boolean isFifoSuggested;
    private List<BatchPalletItemResponse> pallets;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchPalletItemResponse {
        private UUID itemId;
        private String palletCode;
        private String skuCode;
        private String description;
        private String lotNumber;
        private LocalDate expirationDate;
        private Double pieces;
        private String palletTypeId;
        private String palletTypeLabel;
        private String locationCode;
        private String observations;
    }
}
