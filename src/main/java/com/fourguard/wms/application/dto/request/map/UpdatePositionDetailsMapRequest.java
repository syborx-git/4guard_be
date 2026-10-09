package com.fourguard.wms.application.dto.request.map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePositionDetailsMapRequest {
    private String code;
    private String category; // FIXED_STORAGE, TEMPORARY_BUFFER, PRELOAD_STAGING
    private Integer capacityTarimas;
    private String skuCode;
    private String skuDescription;
    private String aisle;
    private String rack;
    private Integer level;
    private String notes;
}
