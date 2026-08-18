package com.fourguard.wms.domain.ports.in;

import com.fourguard.wms.application.dto.request.CreateForkliftOperatorRequest;
import com.fourguard.wms.application.dto.request.UpdateForkliftOperatorRequest;
import com.fourguard.wms.application.dto.request.UpdateForkliftOperatorStatusRequest;
import com.fourguard.wms.application.dto.response.ForkliftOperatorResponse;
import com.fourguard.wms.application.dto.response.audit.ForkliftOperatorAuditResponse;

import java.util.List;
import java.util.UUID;

/**
 * Primary (Driving) Port — Use Case interface for Forklift Operator management (HU-142).
 * Declares all allowed operations from the application's perspective.
 */
public interface ForkliftOperatorUseCase {

    /** Registers a new Forklift Operator in the catalog. */
    ForkliftOperatorResponse createOperator(CreateForkliftOperatorRequest request);

    /** Updates an existing Forklift Operator's data. */
    ForkliftOperatorResponse updateOperator(UpdateForkliftOperatorRequest request);

    /** Returns the full detail of a Forklift Operator by its UUID. */
    ForkliftOperatorResponse getOperatorById(UUID id);

    /**
     * Returns a filtered list of Forklift Operators for an organization.
     *
     * @param organizationId Required. Scopes the result to one organization.
     * @param branchId       Optional. Further filters by branch.
     * @param status         Optional. 'ACTIVO' | 'INACTIVO' | null (all).
     * @param licenseStatus  Optional. 'VIGENTE' | 'POR_VENCER' | 'VENCIDA' | null (all).
     * @param search         Optional. Free-text search against code, fullName, licenseNumberDc3.
     */
    List<ForkliftOperatorResponse> getOperators(
            UUID organizationId,
            UUID branchId,
            String status,
            String licenseStatus,
            String search);

    /** Performs a logical (soft) delete of a Forklift Operator. */
    void deleteOperator(UUID id);

    /**
     * Toggles the Forklift Operator's status between ACTIVO and INACTIVO.
     *
     * @param id      UUID of the operator.
     * @param request Contains the new status and an optional reason.
     */
    ForkliftOperatorResponse updateOperatorStatus(UUID id, UpdateForkliftOperatorStatusRequest request);

    /** Returns the relational audit log history for a Forklift Operator. */
    List<ForkliftOperatorAuditResponse> getOperatorAuditLogs(UUID id);
}
