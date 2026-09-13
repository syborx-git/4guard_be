package com.fourguard.wms.application.usecase;

import com.fourguard.wms.application.dto.request.*;
import com.fourguard.wms.application.dto.response.*;
import com.fourguard.wms.application.mapper.SupplierMapper;
import com.fourguard.wms.domain.enums.SupplierScopeType;
import com.fourguard.wms.domain.enums.SupplierStatus;
import com.fourguard.wms.domain.exception.EntityNotFoundException;
import com.fourguard.wms.domain.exception.ValidationException;
import com.fourguard.wms.domain.ports.in.SupplierUseCase;
import com.fourguard.wms.domain.ports.out.SupplierRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.*;
import com.fourguard.wms.infrastructure.persistence.repository.*;
import com.fourguard.wms.shared.audit.AuditService;
import com.fourguard.wms.shared.audit.SecurityAuditHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupplierService implements SupplierUseCase {

    private final SupplierRepositoryPort supplierRepositoryPort;
    private final OrganizationJpaRepository organizationJpaRepository;
    private final ClientJpaRepository clientJpaRepository;
    private final BranchJpaRepository branchJpaRepository;
    private final CatSupplierTypeJpaRepository catSupplierTypeJpaRepository;
    private final CatCurrencyJpaRepository catCurrencyJpaRepository;
    private final SupplierMapper supplierMapper;
    private final SecurityAuditHelper securityAuditHelper;
    private final AuditService auditService;
    private final com.fourguard.wms.domain.ports.out.AuditLogRepositoryPort auditLogRepositoryPort;
    private final com.fourguard.wms.infrastructure.persistence.repository.UserJpaRepository userJpaRepository;

    // =========================================================================
    // CREATE
    // =========================================================================

    @Override
    @Transactional
    public SupplierResponse createSupplier(CreateSupplierRequest request) {
        log.info("Creating supplier '{}' under org: {}", request.getLegalName(), request.getOrganizationId());

        OrganizationEntity organization = organizationJpaRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new EntityNotFoundException("Organización no encontrada: " + request.getOrganizationId()));

        // Validate unique tax_id per organization
        if (supplierRepositoryPort.existsByOrganizationIdAndTaxIdAndIsDeletedFalse(
                request.getOrganizationId(), request.getTaxId())) {
            throw new ValidationException("Ya existe un proveedor con el RFC/Tax ID '" + request.getTaxId() + "' en esta organización.");
        }

        // Generate PRV-XXXX code
        int nextSeq = supplierRepositoryPort.findMaxCodeSequence(request.getOrganizationId()).orElse(0) + 1;
        String code = "PRV-" + String.format("%04d", nextSeq);

        // Build main entity
        SupplierEntity entity = supplierMapper.toEntity(request);
        entity.setOrganization(organization);
        entity.setCode(code);
        entity.setStatus(SupplierStatus.ACTIVE);
        entity.setIsActive(true);
        entity.setIsDeleted(false);

        // Resolve scope dependencies
        resolveScopeDependencies(entity, request.getScopeType(), request.getClientId(), request.getClientName(), request.getWarehouseId(), request.getWarehouseName());

        // Build and attach 1:1 relations
        String currentUser = securityAuditHelper.getCurrentUsername();
        entity.setCreatedBy(currentUser);
        entity.setUpdatedBy(currentUser);

        if (request.getContact() != null) {
            SupplierContactEntity contact = supplierMapper.toContactEntity(request.getContact());
            entity.setContact(contact);
        }
        if (request.getAddress() != null) {
            SupplierAddressEntity address = supplierMapper.toAddressEntity(request.getAddress());
            entity.setAddress(address);
        }
        if (request.getCommercialTerms() != null) {
            SupplierCommercialTermsEntity terms = supplierMapper.toCommercialTermsEntity(request.getCommercialTerms());
            entity.setCommercialTerms(terms);
        }

        SupplierEntity saved = supplierRepositoryPort.save(entity);

        // Audit log
        logAuditChange(currentUser, "SUPPLIER_CREATED", saved.getId(), null, saved);

        log.info("Supplier created: {} ({})", saved.getCode(), saved.getId());
        return supplierMapper.toResponse(saved);
    }

    // =========================================================================
    // UPDATE
    // =========================================================================

    @Override
    @Transactional
    public SupplierResponse updateSupplier(UUID id, UpdateSupplierRequest request) {
        log.info("Updating supplier: {}", id);

        SupplierEntity existing = findActiveOrThrow(id);
        SupplierEntity snapshot = cloneForAudit(existing);

        // Validate tax_id uniqueness if changed
        if (!existing.getTaxId().equalsIgnoreCase(request.getTaxId()) &&
            supplierRepositoryPort.existsByOrganizationIdAndTaxIdAndIsDeletedFalse(
                    existing.getOrganization().getId(), request.getTaxId())) {
            throw new ValidationException("Ya existe otro proveedor con el RFC/Tax ID '" + request.getTaxId() + "' en esta organización.");
        }

        supplierMapper.updateEntityFromDto(request, existing);

        // Re-resolve scope
        resolveScopeDependencies(existing, request.getScopeType(), request.getClientId(), request.getClientName(), request.getWarehouseId(), request.getWarehouseName());

        // Update 1:1 relations in-place
        if (request.getContact() != null) {
            if (existing.getContact() != null) {
                supplierMapper.updateContactFromDto(request.getContact(), existing.getContact());
            } else {
                SupplierContactEntity contact = supplierMapper.toContactEntity(request.getContact());
                existing.setContact(contact);
            }
        }
        if (request.getAddress() != null) {
            if (existing.getAddress() != null) {
                supplierMapper.updateAddressFromDto(request.getAddress(), existing.getAddress());
            } else {
                SupplierAddressEntity address = supplierMapper.toAddressEntity(request.getAddress());
                existing.setAddress(address);
            }
        }
        if (request.getCommercialTerms() != null) {
            if (existing.getCommercialTerms() != null) {
                supplierMapper.updateCommercialTermsFromDto(request.getCommercialTerms(), existing.getCommercialTerms());
            } else {
                SupplierCommercialTermsEntity terms = supplierMapper.toCommercialTermsEntity(request.getCommercialTerms());
                existing.setCommercialTerms(terms);
            }
        }

        String currentUser = securityAuditHelper.getCurrentUsername();
        existing.setUpdatedBy(currentUser);

        SupplierEntity saved = supplierRepositoryPort.save(existing);

        logAuditChange(currentUser, "SUPPLIER_UPDATED", saved.getId(), snapshot, saved);
        return supplierMapper.toResponse(saved);
    }

    // =========================================================================
    // GET BY ID
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public SupplierResponse getSupplierById(UUID id) {
        log.debug("Fetching supplier: {}", id);
        return supplierMapper.toResponse(findActiveOrThrow(id));
    }

    // =========================================================================
    // GET LIST (Paginated + Filtered)
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public Page<SupplierSummaryResponse> getSuppliers(SupplierFilterRequest filter, Pageable pageable) {
        log.debug("Querying suppliers with filter: {}", filter);

        UUID clientUuid = null;
        if (filter.getClientId() != null && !filter.getClientId().isBlank()) {
            ClientEntity client = resolveClient(filter.getOrganizationId(), filter.getClientId(), null);
            clientUuid = (client != null) ? client.getId() : UUID.randomUUID(); // force mismatch if not found
        }

        UUID warehouseUuid = null;
        if (filter.getWarehouseId() != null && !filter.getWarehouseId().isBlank()) {
            BranchEntity branch = resolveBranch(filter.getOrganizationId(), filter.getWarehouseId(), null);
            warehouseUuid = (branch != null) ? branch.getId() : UUID.randomUUID(); // force mismatch if not found
        }

        Specification<SupplierEntity> spec = SupplierSpecification.fromFilter(filter, clientUuid, warehouseUuid);
        return supplierRepositoryPort.findAll(spec, pageable)
                .map(entity -> {
                    SupplierSummaryResponse summary = supplierMapper.toSummaryResponse(entity);
                    // Map contact summary if available
                    if (entity.getContact() != null) {
                        // Rebuild with contact data (MapStruct toSummaryResponse maps contact field)
                    }
                    return summary;
                });
    }

    // =========================================================================
    // STATUS CHANGE
    // =========================================================================

    @Override
    @Transactional
    public SupplierResponse updateSupplierStatus(UUID id, UpdateSupplierStatusRequest request) {
        SupplierStatus newStatus;
        try {
            newStatus = SupplierStatus.valueOf(request.getStatus().toUpperCase());
        } catch (Exception e) {
            throw new ValidationException("Estado inválido: " + request.getStatus() + ". Valores válidos: ACTIVE, INACTIVE, BLOCKED");
        }

        // Reason is mandatory for INACTIVE and BLOCKED
        if (newStatus != SupplierStatus.ACTIVE &&
            (request.getReason() == null || request.getReason().isBlank())) {
            throw new ValidationException("El motivo del cambio de estado es obligatorio cuando el estado es " + newStatus.name());
        }

        log.info("Changing supplier {} status to {}", id, newStatus);
        SupplierEntity existing = findActiveOrThrow(id);
        SupplierEntity snapshot = cloneForAudit(existing);

        String currentUser = securityAuditHelper.getCurrentUsername();
        existing.setStatus(newStatus);
        existing.setStatusReason(request.getReason());
        existing.setStatusChangedAt(OffsetDateTime.now(ZoneOffset.UTC));
        existing.setStatusChangedBy(currentUser);
        existing.setUpdatedBy(currentUser);

        SupplierEntity saved = supplierRepositoryPort.save(existing);
        logAuditChange(currentUser, "SUPPLIER_STATUS_UPDATED", saved.getId(), snapshot, saved);

        return supplierMapper.toResponse(saved);
    }

    // =========================================================================
    // SOFT DELETE (archive)
    // =========================================================================

    @Override
    @Transactional
    public void deleteSupplier(UUID id) {
        log.info("Archiving supplier: {}", id);
        SupplierEntity existing = findActiveOrThrow(id);
        SupplierEntity snapshot = cloneForAudit(existing);

        String currentUser = securityAuditHelper.getCurrentUsername();
        existing.setIsDeleted(true);
        existing.setIsActive(false);
        existing.setDeletedAt(OffsetDateTime.now(ZoneOffset.UTC));
        existing.setDeletedBy(currentUser);
        existing.setUpdatedBy(currentUser);

        supplierRepositoryPort.save(existing);
        logAuditChange(currentUser, "SUPPLIER_ARCHIVED", id, snapshot, null);
    }

    // =========================================================================
    // AUDIT LOGS
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<SupplierAuditResponse> getSupplierAuditLogs(UUID id) {
        log.debug("Fetching audit logs for supplier: {}", id);
        if (findById(id).isEmpty()) {
            throw new EntityNotFoundException("Proveedor no encontrado: " + id);
        }

        List<AuditLogEntity> logs = auditLogRepositoryPort.findByEntityTypeAndEntityId("SUPPLIER", id);

        return logs.stream().map(logEntry -> {
            String username = resolveUsername(logEntry.getUserId());
            List<SupplierAuditResponse.AuditDetailResponse> details = logEntry.getDetails().stream()
                    .map(d -> SupplierAuditResponse.AuditDetailResponse.builder()
                            .fieldName(d.getFieldName())
                            .oldValue(d.getOldValue())
                            .newValue(d.getNewValue())
                            .build())
                    .collect(Collectors.toList());

            return SupplierAuditResponse.builder()
                    .logId(logEntry.getLogId())
                    .action(logEntry.getAction())
                    .username(username)
                    .createdAt(logEntry.getCreatedAt())
                    .details(details)
                    .build();
        }).collect(Collectors.toList());
    }

    // =========================================================================
    // CATALOGS
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<SupplierTypeResponse> getSupplierTypes() {
        return catSupplierTypeJpaRepository.findByActiveTrueOrderBySortOrderAsc().stream()
                .map(supplierMapper::toTypeResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CurrencyResponse> getCurrencies() {
        return catCurrencyJpaRepository.findByActiveTrueOrderByCodeAsc().stream()
                .map(supplierMapper::toCurrencyResponse)
                .collect(Collectors.toList());
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private SupplierEntity findActiveOrThrow(UUID id) {
        SupplierEntity entity = supplierRepositoryPort.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Proveedor no encontrado: " + id));
        if (Boolean.TRUE.equals(entity.getIsDeleted())) {
            throw new EntityNotFoundException("El proveedor fue archivado y no puede ser modificado: " + id);
        }
        return entity;
    }

    private Optional<SupplierEntity> findById(UUID id) {
        return supplierRepositoryPort.findById(id);
    }

    /**
     * Resolves client and branch references based on scopeType.
     * warehouseId from FE maps to branch_id in the DB (wms.branches).
     */
    private void resolveScopeDependencies(SupplierEntity entity, String scopeTypeStr, String clientId, String clientName, String warehouseId, String warehouseName) {
        SupplierScopeType scopeType;
        try {
            scopeType = SupplierScopeType.valueOf(scopeTypeStr.toUpperCase());
        } catch (Exception e) {
            throw new ValidationException("Scope inválido: " + scopeTypeStr + ". Válidos: GLOBAL, CLIENT, WAREHOUSE");
        }
        entity.setScopeType(scopeType);

        UUID orgId = entity.getOrganization().getId();

        switch (scopeType) {
            case CLIENT -> {
                if (clientId == null || clientId.isBlank()) throw new ValidationException("clientId es requerido cuando scopeType = CLIENT");
                ClientEntity client = resolveClient(orgId, clientId, clientName);
                if (client == null) {
                    throw new EntityNotFoundException("Cliente no encontrado con ID o Código: " + clientId);
                }
                entity.setClient(client);
                entity.setBranch(null);
            }
            case WAREHOUSE -> {
                if (warehouseId == null || warehouseId.isBlank()) throw new ValidationException("warehouseId (branchId) es requerido cuando scopeType = WAREHOUSE");
                BranchEntity branch = resolveBranch(orgId, warehouseId, warehouseName);
                if (branch == null) {
                    throw new EntityNotFoundException("Branch/Almacén no encontrado con ID o Código: " + warehouseId);
                }
                entity.setBranch(branch);
                entity.setClient(null);
            }
            case GLOBAL -> {
                entity.setClient(null);
                entity.setBranch(null);
            }
        }
    }

    private SupplierEntity cloneForAudit(SupplierEntity source) {
        // Shallow clone sufficient for audit state comparison
        return SupplierEntity.builder()
                .id(source.getId())
                .organization(source.getOrganization())
                .code(source.getCode())
                .legalName(source.getLegalName())
                .commercialName(source.getCommercialName())
                .taxId(source.getTaxId())
                .supplierTypeCode(source.getSupplierTypeCode())
                .isPreferred(source.getIsPreferred())
                .status(source.getStatus())
                .statusReason(source.getStatusReason())
                .scopeType(source.getScopeType())
                .notes(source.getNotes())
                .isActive(source.getIsActive())
                .isDeleted(source.getIsDeleted())
                .version(source.getVersion())
                .build();
    }

    private void logAuditChange(String username, String action, UUID entityId,
                                 SupplierEntity before, SupplierEntity after) {
        try {
            UserEntity actor = userJpaRepository.findByUsername(username).orElse(null);
            if (actor != null) {
                Map<String, Object> beforeState = buildAuditState(before);
                Map<String, Object> afterState  = buildAuditState(after);
                auditService.log(actor, action, "SUPPLIER", entityId, beforeState, afterState);
            }
        } catch (Exception e) {
            log.error("Failed to persist audit log for supplier operation '{}': {}", action, e.getMessage());
        }
    }

    private Map<String, Object> buildAuditState(SupplierEntity entity) {
        if (entity == null) return null;
        Map<String, Object> state = new HashMap<>();
        state.put("id",               entity.getId() != null ? entity.getId().toString() : null);
        state.put("code",             entity.getCode());
        state.put("legalName",        entity.getLegalName());
        state.put("commercialName",   entity.getCommercialName());
        state.put("taxId",            entity.getTaxId());
        state.put("supplierTypeCode", entity.getSupplierTypeCode());
        state.put("isPreferred",      entity.getIsPreferred());
        state.put("status",           entity.getStatus() != null ? entity.getStatus().name() : null);
        state.put("statusReason",     entity.getStatusReason());
        state.put("scopeType",        entity.getScopeType() != null ? entity.getScopeType().name() : null);
        state.put("notes",            entity.getNotes());
        state.put("isActive",         entity.getIsActive());
        state.put("isDeleted",        entity.getIsDeleted());
        if (entity.getOrganization() != null) state.put("organizationId", entity.getOrganization().getId().toString());
        if (entity.getClient()       != null) state.put("clientId",       entity.getClient().getId().toString());
        if (entity.getBranch()       != null) state.put("branchId",       entity.getBranch().getId().toString());
        return state;
    }

    private String resolveUsername(UUID userId) {
        if (userId == null) return "SYSTEM";
        return userJpaRepository.findById(userId)
                .map(u -> u.getUsername())
                .orElse("UNKNOWN");
    }

    private BranchEntity resolveBranch(UUID orgId, String warehouseIdStr, String warehouseName) {
        if (warehouseIdStr == null || warehouseIdStr.isBlank()) return null;
        try {
            UUID branchId = UUID.fromString(warehouseIdStr.trim());
            return branchJpaRepository.findById(branchId).orElse(null);
        } catch (IllegalArgumentException e) {
            Optional<BranchEntity> branchOpt = branchJpaRepository.findByOrganizationIdAndCode(orgId, warehouseIdStr.trim());
            if (branchOpt.isPresent()) {
                return branchOpt.get();
            }

            // Auto-create branch for demo/frontend compatibility
            String name = (warehouseName != null && !warehouseName.isBlank()) ? warehouseName : "Branch " + warehouseIdStr;
            log.info("Branch with code '{}' not found. Auto-creating branch '{}' for demo/integration compatibility.", warehouseIdStr, name);
            
            OrganizationEntity org = organizationJpaRepository.findById(orgId)
                    .orElseThrow(() -> new EntityNotFoundException("Organización no encontrada: " + orgId));

            BranchEntity newBranch = BranchEntity.builder()
                    .organization(org)
                    .code(warehouseIdStr.trim())
                    .name(name)
                    .build();
            return branchJpaRepository.save(newBranch);
        }
    }

    private ClientEntity resolveClient(UUID orgId, String clientIdStr, String clientName) {
        if (clientIdStr == null || clientIdStr.isBlank()) return null;
        try {
            UUID clientId = UUID.fromString(clientIdStr.trim());
            return clientJpaRepository.findById(clientId).orElse(null);
        } catch (IllegalArgumentException e) {
            Optional<ClientEntity> clientOpt = clientJpaRepository.findByOrganizationIdAndExternalId(orgId, clientIdStr.trim());
            if (clientOpt.isPresent()) {
                return clientOpt.get();
            }

            // Auto-create client for demo/frontend compatibility
            String name = (clientName != null && !clientName.isBlank()) ? clientName : "Cliente " + clientIdStr;
            log.info("Client with code '{}' not found. Auto-creating client '{}' for demo/integration compatibility.", clientIdStr, name);

            OrganizationEntity org = organizationJpaRepository.findById(orgId)
                    .orElseThrow(() -> new EntityNotFoundException("Organización no encontrada: " + orgId));

            ClientEntity newClient = ClientEntity.builder()
                    .organization(org)
                    .name(name)
                    .externalId(clientIdStr.trim())
                    .build();
            return clientJpaRepository.save(newClient);
        }
    }
}
