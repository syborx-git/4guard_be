package com.fourguard.wms.domain.ports.in;

import com.fourguard.wms.application.dto.request.CreateWarehouseSectionRequest;
import com.fourguard.wms.application.dto.request.InitializeWarehouseSectionRequest;
import com.fourguard.wms.application.dto.request.UpdateWarehouseSectionRequest;
import com.fourguard.wms.application.dto.request.UpdateWarehouseSectionStatusRequest;
import com.fourguard.wms.application.dto.response.WarehouseSectionResponse;
import com.fourguard.wms.application.dto.response.audit.WarehouseSectionAuditResponse;

import java.util.List;
import java.util.UUID;

public interface WarehouseSectionUseCase {
    WarehouseSectionResponse createWarehouseSection(CreateWarehouseSectionRequest request);
    WarehouseSectionResponse updateWarehouseSection(UpdateWarehouseSectionRequest request);
    WarehouseSectionResponse updateWarehouseSectionStatus(UUID id, UpdateWarehouseSectionStatusRequest request);
    WarehouseSectionResponse initializeWarehouseSection(UUID id, InitializeWarehouseSectionRequest request, String username);
    WarehouseSectionResponse getWarehouseSectionById(UUID id);
    List<WarehouseSectionResponse> getWarehouseSectionsByBranchId(UUID branchId);
    List<WarehouseSectionResponse> getAllWarehouseSections();
    void deleteWarehouseSection(UUID id);
    List<WarehouseSectionAuditResponse> getWarehouseSectionAuditLogs(UUID id);
}


