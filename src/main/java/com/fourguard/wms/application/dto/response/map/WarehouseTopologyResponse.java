package com.fourguard.wms.application.dto.response.map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseTopologyResponse {
    private UUID branchId;
    private String branchName;
    private WarehouseMapStatsResponse globalStats;
    private List<WarehouseSectionMapResponse> sections;
    private OffsetDateTime generatedAt;
}
