package com.fourguard.wms.application.dto.response.map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseSectionMapResponse {
    private UUID id;
    private String code;
    private String name;
    private String category;
    private Integer posFijas;
    private Integer capacidadTarimas;
    private String factorEstiba;
    private List<String> materials;
    private String notes;
    private String status; // LOADED | PENDING
    private String polygonPoints;
    private CoordinateResponse labelPosition;
    private CoordinateResponse sublabelPosition;
    private Integer occupiedPositions;
    private Integer availablePositions;
    private Integer blockedPositions;
    private Integer occupancyPercentage;
}
