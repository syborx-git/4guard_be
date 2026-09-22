package com.fourguard.wms.application.dto.response.map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PositionMapDetailResponse {
    private UUID id;
    private Integer positionNumber;
    private String code;
    private UUID sectionId;
    private String sectionName;
    private String skuCode;
    private String skuDescription;
    private String status; // AVAILABLE | OCCUPIED | BLOCKED | MAINTENANCE
    private Integer capacityTarimas;
    private Integer currentTarimas;
    private String batchNumber;
    private String lastMovement;
    private String blockReason;
    private String statusReason;
    private Boolean isBlocked;
}
