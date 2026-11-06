package com.fourguard.wms.domain.ports.in;

import com.fourguard.wms.application.dto.request.transfer.CancelTransferRequest;
import com.fourguard.wms.application.dto.request.transfer.CreateTransferRequest;
import com.fourguard.wms.application.dto.response.reception.MovementAuditResponse;
import com.fourguard.wms.application.dto.response.transfer.TransferResponse;
import com.fourguard.wms.application.dto.response.transfer.TransferSummaryResponse;
import com.fourguard.wms.domain.enums.MovementScope;

import java.util.List;
import java.util.UUID;

/**
 * Primary (Driving) Port — Use Case interface for Warehouse Transfer management (Cambio de Almacén).
 * Homologado con ADR-011, ADR-017, ADR-020 y ADR-022.
 */
public interface WarehouseTransferUseCase {

    /**
     * Registers a new internal pallet relocation.
     * When dispatched with an operator, enters status ASSIGNED and streams via SSE to RF terminals.
     */
    TransferResponse createTransfer(CreateTransferRequest request);

    /**
     * Returns full detail of a transfer including items moved.
     */
    TransferResponse getTransferById(UUID id);

    /**
     * Returns filtered list of transfers for an organization.
     */
    default List<TransferSummaryResponse> getTransfers(
            UUID organizationId,
            UUID branchId,
            String status,
            String search) {
        return getTransfers(organizationId, branchId, status, search, null, MovementScope.ALL);
    }

    List<TransferSummaryResponse> getTransfers(
            UUID organizationId,
            UUID branchId,
            String status,
            String search,
            UUID shiftSessionId,
            MovementScope scope);

    /**
     * Starts execution of a transfer by forklift operator (ASSIGNED -> IN_PROGRESS).
     */
    TransferResponse startTransfer(UUID id, UUID operatorId);

    /**
     * Pauses a transfer due to shift end or urgent task preemption (IN_PROGRESS -> PAUSED).
     */
    TransferResponse pauseTransfer(UUID id, String reason, String notes);

    /**
     * Resumes a paused transfer (PAUSED -> IN_PROGRESS).
     */
    TransferResponse resumeTransfer(UUID id, UUID operatorId);

    /**
     * Completes physical relocation of pallets to destination rack (IN_PROGRESS -> COMPLETED).
     * Updates inventory_items.location_id and creates inventory_movements.
     */
    TransferResponse completeTransfer(UUID id, UUID destinationLocationId);

    /**
     * Updates transfer priority ('NORMAL' or 'URGENT') and broadcasts preemptive SSE event.
     */
    TransferResponse updatePriority(UUID id, String priority);

    /**
     * Reassigns forklift operator dynamically.
     */
    TransferResponse reassignOperator(UUID id, UUID forkliftOperatorId);

    /**
     * Cancels a transfer with admin authorization. Reverts inventory location.
     */
    TransferResponse cancelTransfer(UUID id, CancelTransferRequest request);

    /**
     * Returns the chronological audit log for a transfer folio.
     */
    List<MovementAuditResponse> getAuditLogs(UUID id);
}
