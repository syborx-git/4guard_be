package com.fourguard.wms.application.dto.response.transfer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response DTO for an individual item moved in a transfer.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferItemResponse {

    private UUID id;
    private UUID itemId;
    private String palletCode;
    private String skuCode;
    private String skuDescription;
    private Double pieces;
    private OffsetDateTime createdAt;
}
