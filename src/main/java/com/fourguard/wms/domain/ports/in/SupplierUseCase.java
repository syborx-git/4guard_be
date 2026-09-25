package com.fourguard.wms.domain.ports.in;

import com.fourguard.wms.application.dto.request.CreateSupplierRequest;
import com.fourguard.wms.application.dto.request.SupplierFilterRequest;
import com.fourguard.wms.application.dto.request.UpdateSupplierRequest;
import com.fourguard.wms.application.dto.request.UpdateSupplierStatusRequest;
import com.fourguard.wms.application.dto.response.SupplierAuditResponse;
import com.fourguard.wms.application.dto.response.SupplierResponse;
import com.fourguard.wms.application.dto.response.SupplierSummaryResponse;
import com.fourguard.wms.application.dto.response.SupplierTypeResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/** Port IN — Supplier use case contract. */
public interface SupplierUseCase {

    SupplierResponse createSupplier(CreateSupplierRequest request);

    SupplierResponse updateSupplier(UUID id, UpdateSupplierRequest request);

    SupplierResponse getSupplierById(UUID id);

    Page<SupplierSummaryResponse> getSuppliers(SupplierFilterRequest filter, Pageable pageable);

    SupplierResponse updateSupplierStatus(UUID id, UpdateSupplierStatusRequest request);

    void deleteSupplier(UUID id);

    List<SupplierAuditResponse> getSupplierAuditLogs(UUID id);

    List<SupplierTypeResponse> getSupplierTypes();
}
