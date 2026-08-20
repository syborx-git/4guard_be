package com.fourguard.wms.domain.ports.in;

import com.fourguard.wms.application.dto.request.transfer.CancelTransferRequest;
import com.fourguard.wms.application.dto.request.transfer.CreateTransferRequest;
import com.fourguard.wms.application.dto.response.reception.MovementAuditResponse;
import com.fourguard.wms.application.dto.response.transfer.TransferResponse;
import com.fourguard.wms.application.dto.response.transfer.TransferSummaryResponse;

import java.util.List;
import java.util.UUID;

/**
 * Primary (Driving) Port — Use Case interface for Warehouse Transfer management (Cambio de Almacén).
 */
public interface WarehouseTransferUseCase {

    /**
     * Registers a new internal pallet relocation.
     * Validates: origin has pallets, destination is empty, operator exists.
     * Updates inventory_items.location_id and creates inventory_movements (TRANSFER).
     */
    TransferResponse createTransfer(CreateTransferRequest request);

    /**
     * Returns full detail of a transfer including items moved.
     */
    TransferResponse getTransferById(UUID id);

    /**
     * Returns filtered list of transfers for an organization.
     */
    List<TransferSummaryResponse> getTransfers(
            UUID organizationId,
            UUID branchId,
            String status,
            String search);

    /**
     * Cancels a transfer with admin authorization. Reverts inventory location.
     */
    TransferResponse cancelTransfer(UUID id, CancelTransferRequest request);

    /**
     * Returns the chronological audit log for a transfer folio.
     */
    List<MovementAuditResponse> getAuditLogs(UUID id);
}
