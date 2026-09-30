package com.fourguard.wms.application.dto.response.map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseMapStatsResponse {
    private Integer totalSections;
    private Integer loadedSections;
    private Integer pendingSections;
    private Integer totalPositions;
    private Integer totalCapacityTarimas;
    private Integer occupiedPositions;
    private Integer blockedPositions;
}
