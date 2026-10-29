package com.fourguard.wms.domain.ports.in;

import com.fourguard.wms.application.dto.request.reception.AddReceptionPalletsRequest;
import com.fourguard.wms.application.dto.request.reception.CancelReceptionRequest;
import com.fourguard.wms.application.dto.request.reception.ChangeRemisionRequest;
import com.fourguard.wms.application.dto.request.reception.CompleteReceptionRequest;
import com.fourguard.wms.application.dto.request.reception.CreateCheckInRequest;
import com.fourguard.wms.application.dto.request.reception.ReopenReceptionRequest;
import com.fourguard.wms.application.dto.request.reception.UpdatePalletRequest;
import com.fourguard.wms.application.dto.request.reception.UpdateReceptionParametersRequest;
import com.fourguard.wms.application.dto.response.reception.MovementAuditResponse;
import com.fourguard.wms.application.dto.response.reception.ReceptionPalletResponse;
import com.fourguard.wms.application.dto.response.reception.ReceptionResponse;
import com.fourguard.wms.application.dto.response.reception.ReceptionSummaryResponse;
import com.fourguard.wms.domain.enums.MovementScope;

import java.util.List;
import java.util.UUID;

/**
 * Primary (Driving) Port — Use Case interface for Warehouse Reception management (F01).
 * Declares all allowed operations from the application's perspective.
 */
public interface WarehouseReceptionUseCase {

    ReceptionResponse createCheckIn(CreateCheckInRequest request);

    ReceptionResponse updateParameters(UUID id, UpdateReceptionParametersRequest request);

    ReceptionResponse getReceptionById(UUID id);

    default List<ReceptionSummaryResponse> getReceptions(
            UUID organizationId,
            UUID branchId,
            String status,
            String search) {
        return getReceptions(organizationId, branchId, status, search, null, MovementScope.ALL, null);
    }

    default List<ReceptionSummaryResponse> getReceptions(
            UUID organizationId,
            UUID branchId,
            String status,
            String search,
            UUID shiftSessionId,
            MovementScope scope) {
        return getReceptions(organizationId, branchId, status, search, shiftSessionId, scope, null);
    }

    List<ReceptionSummaryResponse> getReceptions(
            UUID organizationId,
            UUID branchId,
            String status,
            String search,
            UUID shiftSessionId,
            MovementScope scope,
            UUID forkliftOperatorId);

    List<ReceptionPalletResponse> addPallets(UUID receptionId, AddReceptionPalletsRequest request);

    ReceptionPalletResponse updatePallet(UUID receptionId, UUID palletId, UpdatePalletRequest request);

    void deletePallet(UUID receptionId, UUID palletId);

    ReceptionResponse completeReception(UUID id, CompleteReceptionRequest request);

    ReceptionResponse cancelReception(UUID id, CancelReceptionRequest request);

    ReceptionResponse reopenReception(UUID id, ReopenReceptionRequest request);

    ReceptionResponse changeRemision(UUID id, ChangeRemisionRequest request);

    List<MovementAuditResponse> getAuditLogs(UUID id);

    java.util.Map<String, Integer> getNextPalletNumber(UUID organizationId, UUID branchId);

    ReceptionResponse relabelUas(UUID id, com.fourguard.wms.application.dto.request.reception.RelabelUasRequest request);

    List<com.fourguard.wms.infrastructure.persistence.entity.InventoryAuditLogEntity> getRemissionTree(String remissionFolio);

    List<com.fourguard.wms.application.dto.response.reception.ReceptionLotResponse> getLotsByReceptionId(UUID receptionId);

    com.fourguard.wms.application.dto.response.reception.ReceptionLotResponse addLot(
            UUID receptionId,
            com.fourguard.wms.application.dto.request.reception.AddReceptionLotRequest request);

    void deleteLot(UUID receptionId, UUID lotId);

    com.fourguard.wms.application.dto.response.reception.ReturnDetectionResponse detectReturn(
            UUID organizationId,
            UUID branchId,
            String query);

    com.fourguard.wms.application.dto.response.reception.VerifyPalletResponse verifyPallet(
            UUID receptionId,
            com.fourguard.wms.application.dto.request.reception.VerifyPalletRequest request);
}
