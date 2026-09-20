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
     * Updates an outbound dispatch status, assigned ramp/operator, vehicle details, or seal.
     */
    OutboundResponse updateOutbound(UUID id, com.fourguard.wms.application.dto.request.outbound.UpdateOutboundRequest request);

    /**
     * Cancels an outbound dispatch with admin authorization. Reverts inventory state.
     */
    OutboundResponse cancelOutbound(UUID id, CancelOutboundRequest request);

    /**
     * Modifies the remision / carta porte of an outbound dispatch with supervisor authorization.
     */
    OutboundResponse changeRemision(UUID id, ChangeRemisionRequest request);

    /**
     * Returns available inventory batches ordered by FIFO/FEFO for a given client and SKU, with optional search.
     */
    List<InventoryBatchResponse> getInventoryBatches(
            UUID organizationId,
            UUID branchId,
            UUID clientId,
            UUID skuId,
            String search);

    /**
     * Instant RF scanner / barcode lookup for a single pallet by SSCC / UA.
     */
    ScanPalletResponse scanPallet(String barcode, UUID organizationId, UUID branchId);

    /**
     * Validates a batch of scanned barcodes/pallets for rapid mass scanning.
     */
    List<ScanPalletResponse> validatePallets(ValidatePalletsRequest request);

    /**
     * Returns the chronological audit log for an outbound folio.
     */
    List<MovementAuditResponse> getAuditLogs(UUID id);
}
