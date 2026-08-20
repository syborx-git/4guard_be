package com.fourguard.wms.application.dto.response.reception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Response DTO for an individual pallet/UA in a reception.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceptionPalletResponse {

    private UUID id;
    private Integer palletNumber;
    private String palletCode;
    private UUID skuId;
    private String skuCode;
    private String description;
    private UUID supplierId;
    private String supplierName;
    private Double pieces;
    private String palletTypeId;
    private String palletTypeLabel;
    private String observations;
    private UUID inventoryItemId;
}
