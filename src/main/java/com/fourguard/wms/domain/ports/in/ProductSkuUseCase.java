package com.fourguard.wms.domain.ports.in;

import com.fourguard.wms.application.dto.request.CreateProductSkuRequest;
import com.fourguard.wms.application.dto.request.UpdateProductSkuRequest;
import com.fourguard.wms.application.dto.response.ProductSkuResponse;
import com.fourguard.wms.application.dto.response.audit.ProductSkuAuditResponse;

import java.util.List;
import java.util.UUID;

public interface ProductSkuUseCase {
    ProductSkuResponse createProductSku(CreateProductSkuRequest request);
    ProductSkuResponse updateProductSku(UpdateProductSkuRequest request);
    ProductSkuResponse getProductSkuById(UUID id);
    List<ProductSkuResponse> getProductSkusByClientId(UUID clientId);
    List<ProductSkuResponse> getAllProductSkus();
    List<ProductSkuAuditResponse> getProductSkuAuditLogs(UUID id);
    ProductSkuResponse updateProductSkuStatus(UUID id, String status);
    void softDeleteProductSku(UUID id);
    void deleteProductSku(UUID id);
}
