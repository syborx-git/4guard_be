package com.fourguard.wms.application.usecase;

import com.fourguard.wms.application.dto.request.CreateProductSkuRequest;
import com.fourguard.wms.application.dto.request.UpdateProductSkuRequest;
import com.fourguard.wms.application.dto.response.ProductSkuResponse;
import com.fourguard.wms.application.dto.response.audit.ProductSkuAuditResponse;
import com.fourguard.wms.application.mapper.ProductSkuMapper;
import com.fourguard.wms.domain.exception.EntityNotFoundException;
import com.fourguard.wms.domain.exception.ValidationException;
import com.fourguard.wms.domain.ports.in.ProductSkuUseCase;
import com.fourguard.wms.domain.ports.out.AuditLogRepositoryPort;
import com.fourguard.wms.domain.ports.out.ClientRepositoryPort;
import com.fourguard.wms.domain.ports.out.ProductSkuRepositoryPort;
import com.fourguard.wms.domain.ports.out.UserRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.AuditLogEntity;
import com.fourguard.wms.infrastructure.persistence.entity.ClientEntity;
import com.fourguard.wms.infrastructure.persistence.entity.ProductSkuEntity;
import com.fourguard.wms.infrastructure.persistence.entity.UserEntity;
import com.fourguard.wms.shared.audit.AuditService;
import com.fourguard.wms.shared.audit.SecurityAuditHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductSkuService implements ProductSkuUseCase {

    private final ProductSkuRepositoryPort productSkuRepositoryPort;
    private final ClientRepositoryPort clientRepositoryPort;
    private final ProductSkuMapper productSkuMapper;
    private final SecurityAuditHelper securityAuditHelper;
    private final AuditLogRepositoryPort auditLogRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final AuditService auditService;

    @Override
    @Transactional
    public ProductSkuResponse createProductSku(CreateProductSkuRequest request) {
        log.info("Creating SKU: {} for client: {}", request.getCode(), request.getClientId());
        ClientEntity client = clientRepositoryPort.findById(request.getClientId())
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado con ID: " + request.getClientId()));

        if (productSkuRepositoryPort.findByClientIdAndCode(request.getClientId(), request.getCode()).isPresent()) {
            throw new ValidationException("El código SKU ya existe para este cliente: " + request.getCode());
        }

        ProductSkuEntity entity = productSkuMapper.toEntity(request);
        entity.setClient(client);
        String currentUser = securityAuditHelper.getCurrentUsername();
        entity.setCreatedBy(currentUser);
        ProductSkuEntity saved = productSkuRepositoryPort.save(entity);

        logAuditChange(currentUser, "PRODUCT_SKU_CREATED", saved.getId(), null, buildAuditState(saved));
        return productSkuMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ProductSkuResponse updateProductSku(UpdateProductSkuRequest request) {
        log.info("Updating SKU with ID: {}", request.getId());
        ProductSkuEntity existing = productSkuRepositoryPort.findById(request.getId())
                .orElseThrow(() -> new EntityNotFoundException("SKU no encontrado con ID: " + request.getId()));

        ClientEntity client = clientRepositoryPort.findById(request.getClientId())
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado con ID: " + request.getClientId()));

        if (!existing.getCode().equals(request.getCode()) &&
                productSkuRepositoryPort.findByClientIdAndCode(request.getClientId(), request.getCode()).isPresent()) {
            throw new ValidationException("El código SKU ya existe para este cliente: " + request.getCode());
        }

        Map<String, Object> beforeState = buildAuditState(existing);

        productSkuMapper.updateEntityFromDto(request, existing);
        existing.setClient(client);
        String currentUser = securityAuditHelper.getCurrentUsername();
        existing.setUpdatedBy(currentUser);
        ProductSkuEntity saved = productSkuRepositoryPort.save(existing);

        logAuditChange(currentUser, "PRODUCT_SKU_UPDATED", saved.getId(), beforeState, buildAuditState(saved));
        return productSkuMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductSkuResponse getProductSkuById(UUID id) {
        log.debug("Fetching SKU with ID: {}", id);
        return productSkuRepositoryPort.findById(id)
                .map(productSkuMapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("SKU no encontrado con ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSkuResponse> getProductSkusByClientId(UUID clientId) {
        log.debug("Fetching SKUs by client ID: {}", clientId);
        return productSkuRepositoryPort.findByClientId(clientId).stream()
                .map(productSkuMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSkuResponse> getAllProductSkus() {
        log.debug("Fetching all SKUs");
        return productSkuRepositoryPort.findAll().stream()
                .map(productSkuMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteProductSku(UUID id) {
        log.info("Deleting SKU with ID: {}", id);
        ProductSkuEntity existing = productSkuRepositoryPort.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("SKU no encontrado con ID: " + id));

        Map<String, Object> beforeState = buildAuditState(existing);
        String currentUser = securityAuditHelper.getCurrentUsername();

        productSkuRepositoryPort.deleteById(id);
        logAuditChange(currentUser, "PRODUCT_SKU_DELETED", id, beforeState, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSkuAuditResponse> getProductSkuAuditLogs(UUID id) {
        log.debug("Fetching audit logs for SKU: {}", id);
        if (!productSkuRepositoryPort.findById(id).isPresent()) {
            throw new EntityNotFoundException("SKU no encontrado con ID: " + id);
        }

        List<AuditLogEntity> logs = auditLogRepositoryPort.findByEntityTypeAndEntityId("PRODUCT_SKU", id);
        return logs.stream()
                .map(logEntry -> {
                    String username = "SYSTEM";
                    if (logEntry.getUserId() != null) {
                        username = userRepositoryPort.findById(logEntry.getUserId())
                                .map(user -> user.getUsername())
                                .orElse("UNKNOWN");
                    }
                    List<ProductSkuAuditResponse.AuditDetailResponse> details = logEntry.getDetails().stream()
                            .map(d -> ProductSkuAuditResponse.AuditDetailResponse.builder()
                                    .fieldName(d.getFieldName())
                                    .oldValue(d.getOldValue())
                                    .newValue(d.getNewValue())
                                    .build())
                            .collect(Collectors.toList());

                    return ProductSkuAuditResponse.builder()
                            .logId(logEntry.getLogId())
                            .action(logEntry.getAction())
                            .username(username)
                            .createdAt(logEntry.getCreatedAt())
                            .details(details)
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ── Audit Helpers ─────────────────────────────────────────────────────────

    private Map<String, Object> buildAuditState(ProductSkuEntity entity) {
        if (entity == null) return null;
        Map<String, Object> state = new HashMap<>();
        state.put("code", entity.getCode());
        state.put("name", entity.getName());
        state.put("description", entity.getDescription());
        state.put("category", entity.getCategory());
        state.put("weight", entity.getWeight());
        state.put("unit", entity.getUnit());
        if (entity.getClient() != null) {
            state.put("clientId", entity.getClient().getId());
            state.put("clientName", entity.getClient().getName());
        }
        return state;
    }

    private void logAuditChange(String username, String action, UUID entityId, Map<String, Object> beforeState, Map<String, Object> afterState) {
        try {
            UserEntity actor = userRepositoryPort.findByUsername(username).orElse(null);
            if (actor != null) {
                auditService.log(actor, action, "PRODUCT_SKU", entityId, beforeState, afterState);
            }
        } catch (Exception e) {
            log.warn("Failed to write audit log for SKU: {}", entityId, e);
        }
    }
}

