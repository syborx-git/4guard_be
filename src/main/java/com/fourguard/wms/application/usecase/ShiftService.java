package com.fourguard.wms.application.usecase;

import com.fourguard.wms.application.dto.request.CreateShiftRequest;
import com.fourguard.wms.application.dto.request.ShiftFilterRequest;
import com.fourguard.wms.application.dto.request.UpdateShiftRequest;
import com.fourguard.wms.application.dto.request.UpdateShiftStatusRequest;
import com.fourguard.wms.application.dto.response.ShiftResponse;
import com.fourguard.wms.application.dto.response.audit.ShiftAuditResponse;
import com.fourguard.wms.application.mapper.ShiftMapper;
import com.fourguard.wms.domain.enums.ShiftStatus;
import com.fourguard.wms.domain.exception.EntityNotFoundException;
import com.fourguard.wms.domain.exception.ValidationException;
import com.fourguard.wms.domain.ports.in.ShiftUseCase;
import com.fourguard.wms.domain.ports.out.AuditLogRepositoryPort;
import com.fourguard.wms.domain.ports.out.BranchRepositoryPort;
import com.fourguard.wms.domain.ports.out.ShiftRepositoryPort;
import com.fourguard.wms.domain.ports.out.UserRepositoryPort;
import com.fourguard.wms.domain.ports.out.WarehouseSectionRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.AuditLogEntity;
import com.fourguard.wms.infrastructure.persistence.entity.BranchEntity;
import com.fourguard.wms.infrastructure.persistence.entity.ShiftEntity;
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
public class ShiftService implements ShiftUseCase {

    private final ShiftRepositoryPort shiftRepositoryPort;
    private final BranchRepositoryPort branchRepositoryPort;
    private final WarehouseSectionRepositoryPort sectionRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final AuditLogRepositoryPort auditLogRepositoryPort;
    private final ShiftMapper shiftMapper;
    private final SecurityAuditHelper securityAuditHelper;
    private final AuditService auditService;

    @Override
    @Transactional
    public ShiftResponse createShift(CreateShiftRequest request) {
        log.info("Creating shift code: {} for branch: {}", request.getCode(), request.getBranchId());

        BranchEntity branch = null;
        if (request.getBranchId() != null) {
            branch = branchRepositoryPort.findById(request.getBranchId())
                    .orElseThrow(() -> new EntityNotFoundException("Sucursal no encontrada con ID: " + request.getBranchId()));
        }

        WarehouseSectionEntity section = null;
        if (request.getWarehouseSectionId() != null) {
            section = sectionRepositoryPort.findById(request.getWarehouseSectionId())
                    .orElseThrow(() -> new EntityNotFoundException("Sección de almacén no encontrada con ID: " + request.getWarehouseSectionId()));
        }

        if (request.getBranchId() != null && shiftRepositoryPort.existsByCodeAndBranchId(request.getCode(), request.getBranchId())) {
            throw new ValidationException("El código de turno '" + request.getCode() + "' ya existe para esta sucursal.");
        }

        boolean isOvernight = request.getEndTime().isBefore(request.getStartTime());

        // Validate overlaps
        if (request.getBranchId() != null) {
            List<ShiftEntity> overlaps = shiftRepositoryPort.findOverlappingShifts(
                    request.getBranchId(),
                    request.getStartTime(),
                    request.getEndTime(),
                    request.getOperatingDays(),
                    isOvernight,
                    null
            );
            if (!overlaps.isEmpty()) {
                String overlappingCodes = overlaps.stream().map(ShiftEntity::getCode).collect(Collectors.joining(", "));
                throw new ValidationException("El turno genera solapamiento de horarios/días con los siguientes turnos activos: " + overlappingCodes);
            }
        }

        ShiftEntity entity = shiftMapper.toEntity(request);
        entity.setBranch(branch);
        entity.setWarehouseSection(section);
        entity.setIsOvernight(isOvernight);
        entity.setStatus(request.getStatus() != null ? request.getStatus() : ShiftStatus.ACTIVE);

        String currentUser = securityAuditHelper.getCurrentUsername();
        entity.setCreatedBy(currentUser);
        entity.setUpdatedBy(currentUser);

        ShiftEntity saved = shiftRepositoryPort.save(entity);

        // Audit Log
        logAuditChange(currentUser, "SHIFT_CREATED", saved.getId(), null, saved);

        return shiftMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ShiftResponse updateShift(UUID id, UpdateShiftRequest request) {
        log.info("Updating shift ID: {}", id);

        ShiftEntity existing = shiftRepositoryPort.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Turno no encontrado con ID: " + id));

        BranchEntity branch = null;
        if (request.getBranchId() != null) {
            branch = branchRepositoryPort.findById(request.getBranchId())
                    .orElseThrow(() -> new EntityNotFoundException("Sucursal no encontrada con ID: " + request.getBranchId()));
        }

        WarehouseSectionEntity section = null;
        if (request.getWarehouseSectionId() != null) {
            section = sectionRepositoryPort.findById(request.getWarehouseSectionId())
                    .orElseThrow(() -> new EntityNotFoundException("Sección de almacén no encontrada con ID: " + request.getWarehouseSectionId()));
        }

        if (request.getBranchId() != null && shiftRepositoryPort.existsByCodeAndBranchIdAndIdNot(request.getCode(), request.getBranchId(), id)) {
            throw new ValidationException("El código de turno '" + request.getCode() + "' ya existe para esta sucursal.");
        }

        boolean isOvernight = request.getEndTime().isBefore(request.getStartTime());

        if (request.getBranchId() != null) {
            List<ShiftEntity> overlaps = shiftRepositoryPort.findOverlappingShifts(
                    request.getBranchId(),
                    request.getStartTime(),
                    request.getEndTime(),
                    request.getOperatingDays(),
                    isOvernight,
                    id
            );
            if (!overlaps.isEmpty()) {
                String overlappingCodes = overlaps.stream().map(ShiftEntity::getCode).collect(Collectors.joining(", "));
                throw new ValidationException("El turno genera solapamiento de horarios/días con los siguientes turnos activos: " + overlappingCodes);
            }
        }

        ShiftEntity beforeSnapshot = cloneEntity(existing);

        shiftMapper.updateEntityFromDto(request, existing);
        existing.setBranch(branch);
        existing.setWarehouseSection(section);
        existing.setIsOvernight(isOvernight);
        if (request.getStatus() != null) {
            existing.setStatus(request.getStatus());
        }

        String currentUser = securityAuditHelper.getCurrentUsername();
        existing.setUpdatedBy(currentUser);

        ShiftEntity saved = shiftRepositoryPort.save(existing);

        logAuditChange(currentUser, "SHIFT_UPDATED", saved.getId(), beforeSnapshot, saved);

        return shiftMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ShiftResponse updateShiftStatus(UUID id, UpdateShiftStatusRequest request) {
        log.info("Updating status for shift ID: {} to {}", id, request.getStatus());

        ShiftEntity existing = shiftRepositoryPort.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Turno no encontrado con ID: " + id));

        ShiftEntity beforeSnapshot = cloneEntity(existing);
        existing.setStatus(request.getStatus());

        String currentUser = securityAuditHelper.getCurrentUsername();
        existing.setUpdatedBy(currentUser);

        ShiftEntity saved = shiftRepositoryPort.save(existing);

        logAuditChange(currentUser, "SHIFT_STATUS_UPDATED", saved.getId(), beforeSnapshot, saved);

        return shiftMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ShiftResponse getShiftById(UUID id) {
        log.debug("Fetching shift by ID: {}", id);
        return shiftRepositoryPort.findById(id)
                .map(shiftMapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Turno no encontrado con ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShiftResponse> getShifts(ShiftFilterRequest filter) {
        log.debug("Fetching shifts with filter: {}", filter);
        return shiftRepositoryPort.findAll(filter).stream()
                .map(shiftMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteShift(UUID id) {
        log.info("Deleting shift ID: {}", id);

        ShiftEntity existing = shiftRepositoryPort.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Turno no encontrado con ID: " + id));

        ShiftEntity beforeSnapshot = cloneEntity(existing);
        String currentUser = securityAuditHelper.getCurrentUsername();

        shiftRepositoryPort.softDelete(id);

        ShiftEntity afterSnapshot = cloneEntity(existing);
        afterSnapshot.setIsDeleted(true);
        afterSnapshot.setStatus(ShiftStatus.INACTIVE);

        logAuditChange(currentUser, "SHIFT_DELETED", id, beforeSnapshot, afterSnapshot);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShiftAuditResponse> getShiftAuditLogs(UUID id) {
        log.debug("Fetching audit logs for shift ID: {}", id);

        if (shiftRepositoryPort.findById(id).isEmpty()) {
            throw new EntityNotFoundException("Turno no encontrado con ID: " + id);
        }

        List<AuditLogEntity> logs = auditLogRepositoryPort.findByEntityTypeAndEntityId("SHIFT", id);

        return logs.stream()
                .map(logEntry -> {
                    String username = "SYSTEM";
                    if (logEntry.getUserId() != null) {
                        username = userRepositoryPort.findById(logEntry.getUserId())
                                .map(UserEntity::getUsername)
                                .orElse("UNKNOWN");
                    }
                    List<ShiftAuditResponse.AuditDetailResponse> details = logEntry.getDetails().stream()
                            .map(d -> ShiftAuditResponse.AuditDetailResponse.builder()
                                    .fieldName(d.getFieldName())
                                    .oldValue(d.getOldValue())
                                    .newValue(d.getNewValue())
                                    .build())
                            .collect(Collectors.toList());

                    return ShiftAuditResponse.builder()
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

    private ShiftEntity cloneEntity(ShiftEntity source) {
        if (source == null) return null;
        return ShiftEntity.builder()
                .id(source.getId())
                .code(source.getCode())
                .name(source.getName())
                .description(source.getDescription())
                .startTime(source.getStartTime())
                .endTime(source.getEndTime())
                .restBreakMinutes(source.getRestBreakMinutes())
                .toleranceMinutes(source.getToleranceMinutes())
                .isOvernight(source.getIsOvernight())
                .status(source.getStatus())
                .scopeType(source.getScopeType())
                .branch(source.getBranch())
                .warehouseSection(source.getWarehouseSection())
                .operatingDays(source.getOperatingDays() != null ? new java.util.HashSet<>(source.getOperatingDays()) : null)
                .isDeleted(source.getIsDeleted())
                .version(source.getVersion())
                .createdAt(source.getCreatedAt())
                .createdBy(source.getCreatedBy())
                .build();
    }

    private void logAuditChange(String username, String action, UUID entityId, ShiftEntity before, ShiftEntity after) {
        try {
            UserEntity actor = userRepositoryPort.findByUsername(username).orElse(null);
            if (actor != null) {
                Map<String, Object> beforeState = buildAuditState(before);
                Map<String, Object> afterState = buildAuditState(after);
                auditService.log(actor, action, "SHIFT", entityId, beforeState, afterState);
            }
        } catch (Exception e) {
            log.error("Failed to persist audit log for shift operation", e);
        }
    }

    private Map<String, Object> buildAuditState(ShiftEntity entity) {
        if (entity == null) return null;
        Map<String, Object> state = new HashMap<>();
        state.put("id", entity.getId() != null ? entity.getId().toString() : null);
        state.put("code", entity.getCode());
        state.put("name", entity.getName());
        state.put("startTime", entity.getStartTime() != null ? entity.getStartTime().toString() : null);
        state.put("endTime", entity.getEndTime() != null ? entity.getEndTime().toString() : null);
        state.put("restBreakMinutes", entity.getRestBreakMinutes());
        state.put("toleranceMinutes", entity.getToleranceMinutes());
        state.put("isOvernight", entity.getIsOvernight());
        state.put("status", entity.getStatus() != null ? entity.getStatus().name() : null);
        state.put("scopeType", entity.getScopeType() != null ? entity.getScopeType().name() : null);
        state.put("branchId", entity.getBranch() != null ? entity.getBranch().getId().toString() : null);
        state.put("warehouseSectionId", entity.getWarehouseSection() != null ? entity.getWarehouseSection().getId().toString() : null);
        state.put("isDeleted", entity.getIsDeleted());
        return state;
    }
}
