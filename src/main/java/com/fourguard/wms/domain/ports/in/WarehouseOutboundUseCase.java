package com.fourguard.wms.domain.ports.in;

import com.fourguard.wms.application.dto.request.outbound.CancelOutboundRequest;
import com.fourguard.wms.application.dto.request.outbound.CreateOutboundRequest;
import com.fourguard.wms.application.dto.response.outbound.InventoryBatchResponse;
import com.fourguard.wms.application.dto.response.outbound.OutboundResponse;
import com.fourguard.wms.application.dto.response.outbound.OutboundSummaryResponse;
import com.fourguard.wms.application.dto.response.reception.MovementAuditResponse;

import java.util.List;
import java.util.UUID;

/**
 * Primary (Driving) Port — Use Case interface for Warehouse Outbound management (Salidas F03).
 */
public interface WarehouseOutboundUseCase {

    /**
     * Registers a new outbound dispatch.
     * Updates inventory_items.state → DISPATCHED and creates inventory_movements (EXIT).
     */
    OutboundResponse createOutbound(CreateOutboundRequest request);

    /**
     * Returns full detail of an outbound dispatch including items.
     */
    OutboundResponse getOutboundById(UUID id);

    /**
     * Returns filtered list of outbounds for an organization.
     */
    List<OutboundSummaryResponse> getOutbounds(
            UUID organizationId,
            UUID branchId,
            String status,
            String search);

    /**
     * Cancels an outbound dispatch with admin authorization. Reverts inventory state.
     */
    OutboundResponse cancelOutbound(UUID id, CancelOutboundRequest request);

    /**
     * Returns available inventory batches ordered by FIFO/FEFO for a given client and SKU.
     */
    List<InventoryBatchResponse> getInventoryBatches(
            UUID organizationId,
            UUID branchId,
            UUID clientId,
            UUID skuId);

    /**
     * Returns the chronological audit log for an outbound folio.
     */
    List<MovementAuditResponse> getAuditLogs(UUID id);
}
