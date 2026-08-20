package com.fourguard.wms.domain.ports.in;

import com.fourguard.wms.application.dto.request.reception.AddReceptionPalletsRequest;
import com.fourguard.wms.application.dto.request.reception.CancelReceptionRequest;
import com.fourguard.wms.application.dto.request.reception.ChangeRemisionRequest;
import com.fourguard.wms.application.dto.request.reception.CompleteReceptionRequest;
import com.fourguard.wms.application.dto.request.reception.CreateCheckInRequest;
import com.fourguard.wms.application.dto.request.reception.UpdatePalletRequest;
import com.fourguard.wms.application.dto.request.reception.UpdateReceptionParametersRequest;
import com.fourguard.wms.application.dto.response.reception.MovementAuditResponse;
import com.fourguard.wms.application.dto.response.reception.ReceptionPalletResponse;
import com.fourguard.wms.application.dto.response.reception.ReceptionResponse;
import com.fourguard.wms.application.dto.response.reception.ReceptionSummaryResponse;

import java.util.List;
import java.util.UUID;

/**
 * Primary (Driving) Port — Use Case interface for Warehouse Reception management (F01).
 * Declares all allowed operations from the application's perspective.
 */
public interface WarehouseReceptionUseCase {

    /**
     * Registers a new pre-reception from the security checkpoint (Caseta de Seguridad).
     * Generates a consecutive folio and sets status to REGISTERED.
     */
    ReceptionResponse createCheckIn(CreateCheckInRequest request);

    /**
     * Updates the unloading parameters (lot, SKU, pallet type, storage location).
     * Only allowed when status is REGISTERED.
     */
    ReceptionResponse updateParameters(UUID id, UpdateReceptionParametersRequest request);

    /**
     * Returns the full detail of a reception including pallets, seals and audit log.
     */
    ReceptionResponse getReceptionById(UUID id);

    /**
     * Returns a filtered list of receptions for a given organization.
     *
     * @param organizationId Required. Scopes to one organization.
     * @param branchId       Optional. Filters by branch.
     * @param status         Optional. REGISTERED | COMPLETED | CANCELLED
     * @param search         Optional. Free-text against folio, docNumber, clientName.
     */
    List<ReceptionSummaryResponse> getReceptions(
            UUID organizationId,
            UUID branchId,
            String status,
            String search);

    /**
     * Adds one or more pallet/UA codes to an open reception.
     * Validates uniqueness of palletCode within the same reception.
     */
    List<ReceptionPalletResponse> addPallets(UUID receptionId, AddReceptionPalletsRequest request);

    /**
     * Updates an individual pallet record (pieces, type, observations).
     */
    ReceptionPalletResponse updatePallet(UUID receptionId, UUID palletId, UpdatePalletRequest request);

    /**
     * Removes a pallet from an open reception (soft or hard delete).
     */
    void deletePallet(UUID receptionId, UUID palletId);

    /**
     * Closes the reception with leader authorization.
     * Creates inventory_items (state=AVAILABLE) and inventory_movements (type=ENTRY) for each pallet.
     * Validates leader credentials (username + password) against wms.users with role check.
     */
    ReceptionResponse completeReception(UUID id, CompleteReceptionRequest request);

    /**
     * Cancels a reception with mandatory admin authorization and reason.
     * Validates admin credentials against wms.users.
     */
    ReceptionResponse cancelReception(UUID id, CancelReceptionRequest request);

    /**
     * Updates the reception doc number (remisión) with audit trail.
     */
    ReceptionResponse changeRemision(UUID id, ChangeRemisionRequest request);

    /**
     * Returns the chronological audit log for a reception folio.
     */
    List<MovementAuditResponse> getAuditLogs(UUID id);
}
