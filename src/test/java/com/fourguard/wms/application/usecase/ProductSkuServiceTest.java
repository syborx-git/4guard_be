package com.fourguard.wms.application.usecase;

import com.fourguard.wms.application.dto.request.CreateProductSkuRequest;
import com.fourguard.wms.application.dto.request.UpdateProductSkuRequest;
import com.fourguard.wms.application.dto.response.ProductSkuResponse;
import com.fourguard.wms.application.dto.response.audit.ProductSkuAuditResponse;
import com.fourguard.wms.application.mapper.ProductSkuMapper;
import com.fourguard.wms.domain.exception.EntityNotFoundException;
import com.fourguard.wms.domain.exception.ValidationException;
import com.fourguard.wms.domain.ports.out.AuditLogRepositoryPort;
import com.fourguard.wms.domain.ports.out.ClientRepositoryPort;
import com.fourguard.wms.domain.ports.out.ProductSkuRepositoryPort;
import com.fourguard.wms.domain.ports.out.UserRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.AuditLogEntity;
import com.fourguard.wms.infrastructure.persistence.entity.ClientEntity;
import com.fourguard.wms.infrastructure.persistence.entity.ProductSkuEntity;
import com.fourguard.wms.shared.audit.AuditService;
import com.fourguard.wms.shared.audit.SecurityAuditHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductSkuServiceTest {

    @Mock
    private ProductSkuRepositoryPort productSkuRepositoryPort;
    @Mock
    private ClientRepositoryPort clientRepositoryPort;
    @Mock
    private ProductSkuMapper productSkuMapper;
    @Mock
    private SecurityAuditHelper securityAuditHelper;
    @Mock
    private AuditService auditService;
    @Mock
    private AuditLogRepositoryPort auditLogRepositoryPort;
    @Mock
    private UserRepositoryPort userRepositoryPort;

    @InjectMocks
    private ProductSkuService productSkuService;

    private UUID clientId;
    private UUID skuId;
    private ClientEntity clientEntity;
    private ProductSkuEntity skuEntity;
    private ProductSkuResponse skuResponse;
    private CreateProductSkuRequest createRequest;
    private UpdateProductSkuRequest updateRequest;

    @BeforeEach
    void setUp() {
        clientId = UUID.randomUUID();
        skuId = UUID.randomUUID();

        clientEntity = ClientEntity.builder()
                .id(clientId)
                .name("Nestlé México")
                .externalId("CLI-NESTLE")
                .status("ACTIVE")
                .build();

        skuEntity = ProductSkuEntity.builder()
                .id(skuId)
                .client(clientEntity)
                .code("SKU-NES-001")
                .name("Leche Nido 1kg")
                .description("Lata de leche en polvo")
                .weight(new BigDecimal("1.000"))
                .unit("BOX")
                .status("ACTIVE")
                .isDeleted(false)
                .build();

        skuResponse = ProductSkuResponse.builder()
                .id(skuId)
                .clientId(clientId)
                .clientName("Nestlé México")
                .code("SKU-NES-001")
                .name("Leche Nido 1kg")
                .description("Lata de leche en polvo")
                .weight(new BigDecimal("1.000"))
                .unit("BOX")
                .status("ACTIVE")
                .isDeleted(false)
                .build();

        createRequest = CreateProductSkuRequest.builder()
                .clientId(clientId)
                .code("SKU-NES-001")
                .name("Leche Nido 1kg")
                .description("Lata de leche en polvo")
                .weight(new BigDecimal("1.000"))
                .unit("BOX")
                .status("ACTIVE")
                .build();

        updateRequest = UpdateProductSkuRequest.builder()
                .id(skuId)
                .clientId(clientId)
                .code("SKU-NES-001")
                .name("Leche Nido 1kg Actualizada")
                .description("Lata de leche en polvo formato familiar")
                .weight(new BigDecimal("1.200"))
                .unit("BOX")
                .status("ACTIVE")
                .isDeleted(false)
                .build();
    }

    @Test
    void whenCreateProductSku_withValidData_thenSuccess() {
        when(clientRepositoryPort.findById(clientId)).thenReturn(Optional.of(clientEntity));
        when(productSkuRepositoryPort.findByClientIdAndCode(clientId, "SKU-NES-001")).thenReturn(Optional.empty());
        when(productSkuMapper.toEntity(createRequest)).thenReturn(skuEntity);
        when(securityAuditHelper.getCurrentUsername()).thenReturn("admin");
        when(productSkuRepositoryPort.save(any(ProductSkuEntity.class))).thenReturn(skuEntity);
        when(productSkuMapper.toResponse(skuEntity)).thenReturn(skuResponse);

        ProductSkuResponse response = productSkuService.createProductSku(createRequest);

        assertNotNull(response);
        assertEquals("SKU-NES-001", response.getCode());
        verify(productSkuRepositoryPort, times(1)).save(any(ProductSkuEntity.class));
    }

    @Test
    void whenUpdateProductSku_withValidData_thenSuccess() {
        when(productSkuRepositoryPort.findById(skuId)).thenReturn(Optional.of(skuEntity));
        when(clientRepositoryPort.findById(clientId)).thenReturn(Optional.of(clientEntity));
        when(securityAuditHelper.getCurrentUsername()).thenReturn("admin");
        when(productSkuRepositoryPort.save(any(ProductSkuEntity.class))).thenReturn(skuEntity);
        when(productSkuMapper.toResponse(skuEntity)).thenReturn(skuResponse);

        ProductSkuResponse response = productSkuService.updateProductSku(updateRequest);

        assertNotNull(response);
        verify(productSkuRepositoryPort, times(1)).save(any(ProductSkuEntity.class));
    }

    @Test
    void whenUpdateProductSkuStatus_withValidStatus_thenSuccess() {
        when(productSkuRepositoryPort.findById(skuId)).thenReturn(Optional.of(skuEntity));
        when(securityAuditHelper.getCurrentUsername()).thenReturn("admin");
        when(productSkuRepositoryPort.save(any(ProductSkuEntity.class))).thenReturn(skuEntity);
        when(productSkuMapper.toResponse(skuEntity)).thenReturn(skuResponse);

        ProductSkuResponse response = productSkuService.updateProductSkuStatus(skuId, "INACTIVE");

        assertNotNull(response);
        verify(productSkuRepositoryPort, times(1)).save(any(ProductSkuEntity.class));
    }

    @Test
    void whenUpdateProductSkuStatus_withInvalidStatus_thenThrowValidationException() {
        assertThrows(ValidationException.class, () -> productSkuService.updateProductSkuStatus(skuId, "INVALID_STATUS"));
        verify(productSkuRepositoryPort, never()).save(any());
    }

    @Test
    void whenSoftDeleteProductSku_withExistingId_thenSuccess() {
        when(productSkuRepositoryPort.findById(skuId)).thenReturn(Optional.of(skuEntity));
        when(securityAuditHelper.getCurrentUsername()).thenReturn("admin");

        productSkuService.softDeleteProductSku(skuId);

        verify(productSkuRepositoryPort, times(1)).save(argThat(entity -> entity.getIsDeleted() && "INACTIVE".equals(entity.getStatus())));
    }

    @Test
    void whenDeleteProductSku_withExistingId_thenSuccess() {
        when(productSkuRepositoryPort.findById(skuId)).thenReturn(Optional.of(skuEntity));
        when(securityAuditHelper.getCurrentUsername()).thenReturn("admin");

        productSkuService.deleteProductSku(skuId);

        verify(productSkuRepositoryPort, times(1)).deleteById(skuId);
    }

    @Test
    void whenGetProductSkuAuditLogs_withExistingId_thenReturnLogs() {
        when(productSkuRepositoryPort.findById(skuId)).thenReturn(Optional.of(skuEntity));
        AuditLogEntity logEntity = AuditLogEntity.builder()
                .logId(UUID.randomUUID())
                .action("PRODUCT_SKU_CREATED")
                .entityId(skuId)
                .details(List.of())
                .build();
        when(auditLogRepositoryPort.findByEntityTypeAndEntityId("PRODUCT_SKU", skuId)).thenReturn(List.of(logEntity));

        List<ProductSkuAuditResponse> logs = productSkuService.getProductSkuAuditLogs(skuId);

        assertNotNull(logs);
        assertEquals(1, logs.size());
        assertEquals("PRODUCT_SKU_CREATED", logs.get(0).getAction());
        verify(auditLogRepositoryPort, times(1)).findByEntityTypeAndEntityId("PRODUCT_SKU", skuId);
    }
}
