package com.fourguard.wms.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BayOccupancyResponse {
    private UUID id;
    private String code;
    private String name;
    private String zone;
    private String sectionName;
    private String aisle;
    private String rack;
    private String level;
    private String position;
    private Integer capacityPallets;
    private Integer currentStoredPallets;
    private Integer availableUnits;
    private Double occupancyPercentage;
    private String status;
    private Boolean isBlocked;
    private String category;
    private String trafficLight;
    private Boolean isRecommended;
}
