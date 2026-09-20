package com.fourguard.wms.application.usecase;

import com.fourguard.wms.application.dto.request.CreateWarehouseSectionRequest;
import com.fourguard.wms.application.dto.request.UpdateWarehouseSectionRequest;
import com.fourguard.wms.application.dto.request.UpdateWarehouseSectionStatusRequest;
import com.fourguard.wms.application.dto.response.WarehouseSectionResponse;
import com.fourguard.wms.application.dto.response.audit.WarehouseSectionAuditResponse;
import com.fourguard.wms.application.mapper.WarehouseSectionMapper;
import com.fourguard.wms.domain.enums.WarehouseSectionStatus;
import com.fourguard.wms.domain.exception.EntityNotFoundException;
import com.fourguard.wms.domain.ports.in.WarehouseSectionUseCase;
import com.fourguard.wms.domain.ports.out.AuditLogRepositoryPort;
import com.fourguard.wms.domain.ports.out.BranchRepositoryPort;
import com.fourguard.wms.domain.ports.out.UserRepositoryPort;
import com.fourguard.wms.domain.ports.out.WarehouseSectionRepositoryPort;
import com.fourguard.wms.domain.ports.out.LocationRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.AuditLogEntity;
import com.fourguard.wms.infrastructure.persistence.entity.BranchEntity;
import com.fourguard.wms.infrastructure.persistence.entity.LocationEntity;
import com.fourguard.wms.infrastructure.persistence.entity.UserEntity;
import com.fourguard.wms.infrastructure.persistence.entity.WarehouseSectionEntity;
import com.fourguard.wms.shared.audit.AuditService;
import com.fourguard.wms.shared.audit.SecurityAuditHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WarehouseSectionService implements WarehouseSectionUseCase {

    private final WarehouseSectionRepositoryPort sectionRepositoryPort;
    private final BranchRepositoryPort branchRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final AuditLogRepositoryPort auditLogRepositoryPort;
    private final LocationRepositoryPort locationRepositoryPort;
    private final WarehouseSectionMapper sectionMapper;
    private final SecurityAuditHelper securityAuditHelper;
    private final AuditService auditService;


    @Override
    @Transactional
    public WarehouseSectionResponse createWarehouseSection(CreateWarehouseSectionRequest request) {
        log.info("Creating warehouse section with code: {} for branch: {}", request.getCode(), request.getBranchId());
        BranchEntity branch = branchRepositoryPort.findById(request.getBranchId())
                .orElseThrow(() -> new EntityNotFoundException("Sucursal no encontrada con ID: " + request.getBranchId()));

        WarehouseSectionEntity entity = sectionMapper.toEntity(request);
        entity.setBranch(branch);

        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        } else {
            entity.setStatus(WarehouseSectionStatus.ACTIVE);
        }

        String currentUser = securityAuditHelper.getCurrentUsername();
        entity.setCreatedBy(currentUser);
        entity.setUpdatedBy(currentUser);

        WarehouseSectionEntity saved = sectionRepositoryPort.save(entity);

        // Audit log
        logAuditChange(currentUser, "SECTION_CREATED", saved.getId(), null, saved);

        return sectionMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public WarehouseSectionResponse updateWarehouseSection(UpdateWarehouseSectionRequest request) {
        log.info("Updating warehouse section with ID: {}", request.getId());
        WarehouseSectionEntity existing = sectionRepositoryPort.findById(request.getId())
                .orElseThrow(() -> new EntityNotFoundException("Sección no encontrada con ID: " + request.getId()));

        BranchEntity branch = branchRepositoryPort.findById(request.getBranchId())
                .orElseThrow(() -> new EntityNotFoundException("Sucursal no encontrada con ID: " + request.getBranchId()));

        // Take snapshot for audit
        WarehouseSectionEntity originalSnapshot = cloneEntity(existing);

        sectionMapper.updateEntityFromDto(request, existing);
        existing.setBranch(branch);

        if (request.getStatus() != null) {
            existing.setStatus(request.getStatus());
        } else if (existing.getStatus() == null) {
            existing.setStatus(WarehouseSectionStatus.ACTIVE);
        }

        String currentUser = securityAuditHelper.getCurrentUsername();
        existing.setUpdatedBy(currentUser);

        WarehouseSectionEntity saved = sectionRepositoryPort.save(existing);

        // Audit log
        logAuditChange(currentUser, "SECTION_UPDATED", saved.getId(), originalSnapshot, saved);

        return sectionMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public WarehouseSectionResponse updateWarehouseSectionStatus(UUID id, UpdateWarehouseSectionStatusRequest request) {
        log.info("Updating status for warehouse section ID={} to {}", id, request.getStatus());
        WarehouseSectionEntity existing = sectionRepositoryPort.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Sección no encontrada con ID: " + id));

        WarehouseSectionEntity originalSnapshot = cloneEntity(existing);
        existing.setStatus(request.getStatus());

        String currentUser = securityAuditHelper.getCurrentUsername();
        existing.setUpdatedBy(currentUser);

        WarehouseSectionEntity saved = sectionRepositoryPort.save(existing);

        // Audit log with action "SECTION_STATUS_UPDATED"
        logAuditChange(currentUser, "SECTION_STATUS_UPDATED", saved.getId(), originalSnapshot, saved);

        return sectionMapper.toResponse(saved);
    }


    @Override
    @Transactional(readOnly = true)
    public WarehouseSectionResponse getWarehouseSectionById(UUID id) {
        log.debug("Fetching warehouse section with ID: {}", id);
        return sectionRepositoryPort.findById(id)
                .map(sectionMapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Sección no encontrada con ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarehouseSectionResponse> getWarehouseSectionsByBranchId(UUID branchId) {
        log.debug("Fetching warehouse sections by branch ID: {}", branchId);
        return sectionRepositoryPort.findByBranchId(branchId).stream()
                .map(sectionMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WarehouseSectionResponse> getAllWarehouseSections() {
        log.debug("Fetching all warehouse sections");
        return sectionRepositoryPort.findAll().stream()
                .map(sectionMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteWarehouseSection(UUID id) {
        log.info("Logical delete and cascade for warehouse section ID: {}", id);
        WarehouseSectionEntity existing = sectionRepositoryPort.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Sección no encontrada con ID: " + id));

        WarehouseSectionEntity originalSnapshot = cloneEntity(existing);
        String currentUser = securityAuditHelper.getCurrentUsername();

        // 1. Cascada: Desasociar ubicaciones vinculadas a esta sección
        List<LocationEntity> associatedLocations = locationRepositoryPort.findBySectionId(id);
        if (!associatedLocations.isEmpty()) {
            log.info("Unlinking {} locations from section ID: {}", associatedLocations.size(), id);
            for (LocationEntity location : associatedLocations) {
                location.setSection(null);
                locationRepositoryPort.save(location);
            }
        }

        // 2. Borrado Lógico: Marcar estatus como INACTIVE
        existing.setStatus(WarehouseSectionStatus.INACTIVE);
        existing.setUpdatedBy(currentUser);
        WarehouseSectionEntity saved = sectionRepositoryPort.save(existing);

        // 3. Bitácora de Auditoría
        logAuditChange(currentUser, "SECTION_DELETED", id, originalSnapshot, saved);
    }


    @Override
    @Transactional(readOnly = true)
    public List<WarehouseSectionAuditResponse> getWarehouseSectionAuditLogs(UUID id) {
        log.debug("Fetching audit logs for warehouse section: {}", id);
        if (!sectionRepositoryPort.findById(id).isPresent()) {
            throw new EntityNotFoundException("Sección no encontrada con ID: " + id);
        }

        List<AuditLogEntity> logs = auditLogRepositoryPort.findByEntityTypeAndEntityId("WAREHOUSE_SECTION", id);

        return logs.stream()
                .map(logEntry -> {
                    String username = "SYSTEM";
                    if (logEntry.getUserId() != null) {
                        username = userRepositoryPort.findById(logEntry.getUserId())
                                .map(UserEntity::getUsername)
                                .orElse("UNKNOWN");
                    }
                    List<WarehouseSectionAuditResponse.AuditDetailResponse> detailResponses = logEntry.getDetails().stream()
                            .map(d -> WarehouseSectionAuditResponse.AuditDetailResponse.builder()
                                    .fieldName(d.getFieldName())
                                    .oldValue(d.getOldValue())
                                    .newValue(d.getNewValue())
                                    .build())
                            .collect(Collectors.toList());

                    return WarehouseSectionAuditResponse.builder()
                            .logId(logEntry.getLogId())
                            .action(logEntry.getAction())
                            .username(username)
                            .createdAt(logEntry.getCreatedAt())
                            .details(detailResponses)
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ── Audit Helpers ─────────────────────────────────────────────────────────

    private WarehouseSectionEntity cloneEntity(WarehouseSectionEntity source) {
        if (source == null) return null;
        return WarehouseSectionEntity.builder()
                .id(source.getId())
                .branch(source.getBranch())
                .code(source.getCode())
                .name(source.getName())
                .status(source.getStatus())
                .version(source.getVersion())
                .createdAt(source.getCreatedAt())
                .createdBy(source.getCreatedBy())
                .build();
    }

    private void logAuditChange(String username, String action, UUID entityId, WarehouseSectionEntity before, WarehouseSectionEntity after) {
        try {
            UserEntity actor = userRepositoryPort.findByUsername(username).orElse(null);
            if (actor != null) {
                Map<String, Object> beforeState = buildAuditState(before);
                Map<String, Object> afterState = buildAuditState(after);
                auditService.log(actor, action, "WAREHOUSE_SECTION", entityId, beforeState, afterState);
            }
        } catch (Exception e) {
            log.error("Failed to persist audit log for warehouse section operation", e);
        }
    }

    private Map<String, Object> buildAuditState(WarehouseSectionEntity entity) {
        if (entity == null) return null;
        Map<String, Object> state = new HashMap<>();
        state.put("id", entity.getId() != null ? entity.getId().toString() : null);
        state.put("code", entity.getCode());
        state.put("name", entity.getName());
        state.put("status", entity.getStatus() != null ? entity.getStatus().name() : null);
        state.put("branchId", entity.getBranch() != null ? entity.getBranch().getId().toString() : null);
        return state;
    }
}
