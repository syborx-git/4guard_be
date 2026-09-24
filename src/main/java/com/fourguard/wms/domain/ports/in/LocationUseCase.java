package com.fourguard.wms.domain.ports.in;

import com.fourguard.wms.application.dto.request.CreateLocationRequest;
import com.fourguard.wms.application.dto.request.UpdateLocationRequest;
import com.fourguard.wms.application.dto.request.UpdateLocationStatusRequest;
import com.fourguard.wms.application.dto.response.LocationResponse;
import com.fourguard.wms.application.dto.response.audit.LocationAuditResponse;

import java.util.List;
import java.util.UUID;

public interface LocationUseCase {
    LocationResponse createLocation(CreateLocationRequest request);
    LocationResponse updateLocation(UpdateLocationRequest request);
    LocationResponse getLocationById(UUID id);
    List<LocationResponse> getLocationsByBranchId(UUID branchId);
    List<LocationResponse> getAvailableLocationsByBranchId(UUID branchId);
    List<LocationResponse> getAllLocations();
    void deleteLocation(UUID id);

    /**
     * FSM status change for a location.
     * Validates transition rules, reason requirements, and occupancy constraints.
     *
     * @param id      location UUID
     * @param request new status + optional reason
     * @return updated location response
     */
    LocationResponse changeLocationStatus(UUID id, UpdateLocationStatusRequest request);

    /**
     * Retrieves the audit log history for a specific location.
     *
     * @param id location UUID
     * @return list of audit log entries for the location
     */
    List<LocationAuditResponse> getLocationAuditLogs(UUID id);

    /**
     * Returns the occupancy calculation and traffic-light status for all bays in a branch
     * using the standard 22-pallet capacity threshold.
     */
    List<com.fourguard.wms.application.dto.response.BayOccupancyResponse> getBayOccupancyList(UUID branchId);
}

