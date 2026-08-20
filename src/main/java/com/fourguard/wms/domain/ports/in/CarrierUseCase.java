package com.fourguard.wms.domain.ports.in;

import com.fourguard.wms.application.dto.request.CreateCarrierRequest;
import com.fourguard.wms.application.dto.request.UpdateCarrierRequest;
import com.fourguard.wms.application.dto.request.UpdateCarrierStatusRequest;
import com.fourguard.wms.application.dto.response.CarrierResponse;
import com.fourguard.wms.application.dto.response.audit.CarrierAuditResponse;
import com.fourguard.wms.domain.enums.CarrierStatus;

import java.util.List;
import java.util.UUID;

public interface CarrierUseCase {
    CarrierResponse createCarrier(CreateCarrierRequest request);
    CarrierResponse updateCarrier(UpdateCarrierRequest request);
    CarrierResponse getCarrierById(UUID id);
    List<CarrierResponse> getCarriersByOrganizationId(UUID organizationId);
    List<CarrierResponse> getAllCarriers();
    void deleteCarrier(UUID id);
    CarrierResponse updateCarrierStatus(UUID id, UpdateCarrierStatusRequest request);
    List<CarrierAuditResponse> getCarrierAuditLogs(UUID id);
    void validateTaxId(String taxId, UUID organizationId, UUID excludeId);
}
