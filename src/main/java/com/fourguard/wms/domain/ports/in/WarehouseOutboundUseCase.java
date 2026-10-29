package com.fourguard.wms.domain.ports.in;

import com.fourguard.wms.application.dto.request.outbound.CancelOutboundRequest;
import com.fourguard.wms.application.dto.request.outbound.CreateOutboundRequest;
import com.fourguard.wms.application.dto.request.outbound.ValidatePalletsRequest;
import com.fourguard.wms.application.dto.request.reception.ChangeRemisionRequest;
import com.fourguard.wms.application.dto.response.outbound.InventoryBatchResponse;
import com.fourguard.wms.application.dto.response.outbound.OutboundResponse;
import com.fourguard.wms.application.dto.response.outbound.OutboundSummaryResponse;
import com.fourguard.wms.application.dto.response.outbound.ScanPalletResponse;
import com.fourguard.wms.application.dto.response.reception.MovementAuditResponse;
import com.fourguard.wms.domain.enums.MovementScope;

import java.util.List;
import java.util.UUID;

/**
 * Primary (Driving) Port — Use Case interface for Warehouse Outbound management (Salidas F03).
 */
public interface WarehouseOutboundUseCase {

    OutboundResponse createOutbound(CreateOutboundRequest request);

    OutboundResponse getOutboundById(UUID id);

    default List<OutboundSummaryResponse> getOutbounds(
            UUID organizationId,
            UUID branchId,
            String status,
            String search) {
        return getOutbounds(organizationId, branchId, status, search, null, MovementScope.ALL, null);
    }

    default List<OutboundSummaryResponse> getOutbounds(
            UUID organizationId,
            UUID branchId,
            String status,
            String search,
            UUID shiftSessionId,
            MovementScope scope) {
        return getOutbounds(organizationId, branchId, status, search, shiftSessionId, scope, null);
    }

    List<OutboundSummaryResponse> getOutbounds(
            UUID organizationId,
            UUID branchId,
            String status,
            String search,
            UUID shiftSessionId,
            MovementScope scope,
            UUID forkliftOperatorId);

    OutboundResponse updateOutbound(UUID id, com.fourguard.wms.application.dto.request.outbound.UpdateOutboundRequest request);

    OutboundResponse cancelOutbound(UUID id, CancelOutboundRequest request);

    OutboundResponse changeRemision(UUID id, ChangeRemisionRequest request);

    List<InventoryBatchResponse> getInventoryBatches(
            UUID organizationId,
            UUID branchId,
            UUID clientId,
            UUID skuId,
            String search);

    ScanPalletResponse scanPallet(String barcode, UUID organizationId, UUID branchId);

    List<ScanPalletResponse> validatePallets(ValidatePalletsRequest request);

    List<MovementAuditResponse> getAuditLogs(UUID id);
}
