package com.fourguard.wms.application.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Response DTO for Warehouse Section. */
@Getter
@Builder
public class WarehouseSectionResponse {
    private final UUID id;
    private final UUID branchId;
    private final String branchName;
    private final String code;
    private final String name;
    private final String status;
    private final String category;
    private final Integer posFijas;
    private final Integer capacidadTarimas;
    private final String factorEstiba;
    private final String notes;
    private final String polygonPoints;
    private final Long version;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;
}

