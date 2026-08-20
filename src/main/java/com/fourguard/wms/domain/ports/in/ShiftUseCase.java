package com.fourguard.wms.domain.ports.in;

import com.fourguard.wms.application.dto.request.CreateShiftRequest;
import com.fourguard.wms.application.dto.request.ShiftFilterRequest;
import com.fourguard.wms.application.dto.request.UpdateShiftRequest;
import com.fourguard.wms.application.dto.request.UpdateShiftStatusRequest;
import com.fourguard.wms.application.dto.response.ShiftResponse;
import com.fourguard.wms.application.dto.response.audit.ShiftAuditResponse;

import java.util.List;
import java.util.UUID;

public interface ShiftUseCase {

    ShiftResponse createShift(CreateShiftRequest request);

    ShiftResponse updateShift(UUID id, UpdateShiftRequest request);

    ShiftResponse updateShiftStatus(UUID id, UpdateShiftStatusRequest request);

    ShiftResponse getShiftById(UUID id);

    List<ShiftResponse> getShifts(ShiftFilterRequest filter);

    void deleteShift(UUID id);

    List<ShiftAuditResponse> getShiftAuditLogs(UUID id);
}
