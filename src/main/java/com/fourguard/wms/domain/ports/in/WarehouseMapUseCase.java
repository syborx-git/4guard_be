package com.fourguard.wms.domain.ports.in;

import com.fourguard.wms.application.dto.request.map.CreatePositionMapRequest;
import com.fourguard.wms.application.dto.request.map.UpdatePositionDetailsMapRequest;
import com.fourguard.wms.application.dto.request.map.UpdatePositionStatusMapRequest;
import com.fourguard.wms.application.dto.response.map.CatBlockReasonResponse;
import com.fourguard.wms.application.dto.response.map.PositionMapDetailResponse;
import com.fourguard.wms.application.dto.response.map.WarehouseTopologyResponse;

import java.util.List;
import java.util.UUID;

public interface WarehouseMapUseCase {
    WarehouseTopologyResponse getTopology(UUID branchId);
    List<PositionMapDetailResponse> getPositionsBySection(UUID sectionId, String status, String search);
    List<PositionMapDetailResponse> getAllPositions(UUID branchId, UUID sectionId, String status, String search);
    PositionMapDetailResponse updatePositionStatus(UUID positionId, UpdatePositionStatusMapRequest request, String username);
    PositionMapDetailResponse createPosition(CreatePositionMapRequest request, String username);
    PositionMapDetailResponse updatePositionDetails(UUID positionId, UpdatePositionDetailsMapRequest request, String username);
    void deletePosition(UUID positionId, String username);
    List<CatBlockReasonResponse> getActiveBlockReasons();
}

