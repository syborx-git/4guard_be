package com.fourguard.wms.application.dto.request.reception;

import lombok.Data;

/**
 * Request DTO for editing an individual pallet in an open reception.
 */
@Data
public class UpdatePalletRequest {
    private Double pieces;
    private String palletType;
    private String observations;
}
