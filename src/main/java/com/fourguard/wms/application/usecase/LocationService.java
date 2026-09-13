package com.fourguard.wms.application.usecase;

import com.fourguard.wms.application.dto.request.CreateLocationRequest;
import com.fourguard.wms.application.dto.request.UpdateLocationRequest;
import com.fourguard.wms.application.dto.request.UpdateLocationStatusRequest;
import com.fourguard.wms.application.dto.response.LocationResponse;
import com.fourguard.wms.application.mapper.LocationMapper;
import com.fourguard.wms.domain.enums.LocationStatus;
import com.fourguard.wms.domain.exception.ConflictException;
import com.fourguard.wms.domain.exception.EntityNotFoundException;
import com.fourguard.wms.domain.exception.InvalidFsmTransitionException;
import com.fourguard.wms.domain.exception.ValidationException;
import com.fourguard.wms.application.dto.response.audit.LocationAuditResponse;
import com.fourguard.wms.domain.ports.in.LocationUseCase;
import com.fourguard.wms.domain.ports.out.AuditLogRepositoryPort;
import com.fourguard.wms.domain.ports.out.BranchRepositoryPort;
import com.fourguard.wms.domain.ports.out.LocationRepositoryPort;
import com.fourguard.wms.domain.ports.out.UserRepositoryPort;
import com.fourguard.wms.domain.ports.out.WarehouseSectionRepositoryPort;
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
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocationService implements LocationUseCase {

    private final LocationRepositoryPort locationRepositoryPort;
    private final BranchRepositoryPort branchRepositoryPort;
    private final WarehouseSectionRepositoryPort sectionRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final AuditLogRepositoryPort auditLogRepositoryPort;
    private final LocationMapper locationMapper;
    private final SecurityAuditHelper securityAuditHelper;
    private final AuditService auditService;

    // =========================================================================
    // CREATE
    // =========================================================================

    @Override
    @Transactional
    public LocationResponse createLocation(CreateLocationRequest request) {
        log.info("Creating location zone={} type={} branch={}", request.getZone(), request.getType(), request.getBranchId());

        BranchEntity branch = branchRepositoryPort.findById(request.getBranchId())
                .orElseThrow(() -> new EntityNotFoundException("Sucursal no encontrada con ID: " + request.getBranchId()));

        WarehouseSectionEntity section = null;
        if (request.getSectionId() != null) {
            section = sectionRepositoryPort.findById(request.getSectionId())
                    .orElseThrow(() -> new EntityNotFoundException("Sección no encontrada con ID: " + request.getSectionId()));
        }

        LocationEntity entity = locationMapper.toEntity(request);
        entity.setBranch(branch);
        entity.setSection(section);
        entity.setCurrentOccupancy(0);

        // FSM: new locations always start as ACTIVE
        entity.setStatus(LocationStatus.ACTIVE);

        // The DB trigger will set is_blocked=false and block_reason=null automatically,
        // but we set them explicitly to keep the JPA entity consistent.
        entity.setIsBlocked(false);
        entity.setBlockReason(null);

        String currentUser = securityAuditHelper.getCurrentUsername();
        entity.setCreatedBy(currentUser);

        LocationEntity saved = locationRepositoryPort.save(entity);

        // Audit log
        logAuditChange(currentUser, "LOCATION_CREATED", saved.getId(), null, saved);

        return locationMapper.toResponse(saved);
    }

    // =========================================================================
    // UPDATE
    // =========================================================================

    @Override
    @Transactional
    public LocationResponse updateLocation(UpdateLocationRequest request) {
        log.info("Updating location ID={}", request.getId());

        LocationEntity existing = locationRepositoryPort.findById(request.getId())
                .orElseThrow(() -> new EntityNotFoundException("Ubicación no encontrada con ID: " + request.getId()));

        BranchEntity branch = branchRepositoryPort.findById(request.getBranchId())
                .orElseThrow(() -> new EntityNotFoundException("Sucursal no encontrada con ID: " + request.getBranchId()));

        WarehouseSectionEntity section = null;
        if (request.getSectionId() != null) {
            section = sectionRepositoryPort.findById(request.getSectionId())
                    .orElseThrow(() -> new EntityNotFoundException("Sección no encontrada con ID: " + request.getSectionId()));
        }

        // ── Code uniqueness validation ──────────────────────────────────────
        if (StringUtils.hasText(request.getCode())) {
            boolean codeTaken = locationRepositoryPort.existsByCodeAndIdNot(request.getCode(), request.getId());
            if (codeTaken) {
                throw new ConflictException(
                    "El código '" + request.getCode() + "' ya está asignado a otra ubicación.");
            }
        }

        // Take snapshot for audit
        LocationEntity originalSnapshot = cloneLocationEntity(existing);

        locationMapper.updateEntityFromDto(request, existing);
        existing.setBranch(branch);
        existing.setSection(section);

        // NOTE: status is NOT modified here — use PATCH /locations/{id}/status
        // Legacy isBlocked/blockReason remain in sync via the DB trigger.

        String currentUser = securityAuditHelper.getCurrentUsername();
        existing.setUpdatedBy(currentUser);
        LocationEntity saved = locationRepositoryPort.save(existing);

        // Audit log
        logAuditChange(currentUser, "LOCATION_UPDATED", saved.getId(), originalSnapshot, saved);

        return locationMapper.toResponse(saved);
    }

    // =========================================================================
    // FSM STATUS CHANGE
    // =========================================================================

    @Override
    @Transactional
    public LocationResponse changeLocationStatus(UUID id, UpdateLocationStatusRequest request) {
        log.info("Changing status for location ID={} to {}", id, request.getStatus());

        LocationEntity existing = locationRepositoryPort.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Ubicación no encontrada con ID: " + id));

        LocationEntity originalSnapshot = cloneLocationEntity(existing);

        LocationStatus currentStatus = existing.getStatus() != null ? existing.getStatus() : LocationStatus.ACTIVE;
        LocationStatus targetStatus  = request.getStatus();

        // ── 1. Validate FSM transition ──────────────────────────────────────
        if (!currentStatus.canTransitionTo(targetStatus)) {
            throw new InvalidFsmTransitionException(
                "Transición de estado inválida: no se puede cambiar de " +
                currentStatus.name() + " a " + targetStatus.name() + " directamente.");
        }

        // ── 2. Validate mandatory reason ────────────────────────────────────
        if (LocationStatus.requiresReason(targetStatus) && !StringUtils.hasText(request.getReason())) {
            throw new ValidationException(
                "El campo 'reason' es obligatorio para el estado " + targetStatus.name() + ".");
        }

        // ── 3. Validate occupancy constraint for INACTIVE ────────────────────
        if (targetStatus == LocationStatus.INACTIVE) {
            int occupancy = existing.getCurrentOccupancy() != null ? existing.getCurrentOccupancy() : 0;
            if (occupancy > 0) {
                throw new ConflictException(
                    "No se puede desactivar la ubicación porque tiene inventario activo (currentOccupancy: " + occupancy + ").");
            }
        }

        // ── 4. Apply change ─────────────────────────────────────────────────
        existing.setStatus(targetStatus);
        existing.setStatusReason(StringUtils.hasText(request.getReason()) ? request.getReason() : null);

        // The DB trigger handles is_blocked and block_reason synchronization.
        // We also set them here so the JPA entity is consistent within the same transaction.
        existing.setIsBlocked(targetStatus == LocationStatus.BLOCKED);
        existing.setBlockReason(targetStatus == LocationStatus.BLOCKED ? request.getReason() : null);

        String currentUser = securityAuditHelper.getCurrentUsername();
        existing.setUpdatedBy(currentUser);

        LocationEntity saved = locationRepositoryPort.save(existing);

        // Audit log
        logAuditChange(currentUser, "LOCATION_STATUS_UPDATED", saved.getId(), originalSnapshot, saved);

        log.info("Location ID={} status changed from {} to {}", id, currentStatus, targetStatus);
        return locationMapper.toResponse(saved);
    }

    // =========================================================================
    // READ
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public LocationResponse getLocationById(UUID id) {
        log.debug("Fetching location ID={}", id);
        return locationRepositoryPort.findById(id)
                .map(locationMapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Ubicación no encontrada con ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocationResponse> getLocationsByBranchId(UUID branchId) {
        log.debug("Fetching locations by branchId={}", branchId);
        return locationRepositoryPort.findByBranchId(branchId).stream()
                .map(locationMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocationResponse> getAvailableLocationsByBranchId(UUID branchId) {
        log.debug("Fetching available locations by branchId={}", branchId);
        return locationRepositoryPort.findAvailableByBranchId(branchId).stream()
                .map(locationMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocationResponse> getAllLocations() {
        log.debug("Fetching all locations");
        return locationRepositoryPort.findAll().stream()
                .map(locationMapper::toResponse)
                .collect(Collectors.toList());
    }

    // =========================================================================
    // DELETE
    // =========================================================================

    @Override
    @Transactional
    public void deleteLocation(UUID id) {
        log.info("Deleting location ID={}", id);
        LocationEntity existing = locationRepositoryPort.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Ubicación no encontrada con ID: " + id));

        LocationEntity originalSnapshot = cloneLocationEntity(existing);
        String currentUser = securityAuditHelper.getCurrentUsername();

        // Log delete audit before actual removal
        logAuditChange(currentUser, "LOCATION_DELETED", id, originalSnapshot, null);

        locationRepositoryPort.deleteById(id);
    }

    // =========================================================================
    // AUDIT LOGS
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<LocationAuditResponse> getLocationAuditLogs(UUID id) {
        log.debug("Fetching audit logs for location: {}", id);
        if (locationRepositoryPort.findById(id).isEmpty()) {
            throw new EntityNotFoundException("Ubicación no encontrada con ID: " + id);
        }

        List<AuditLogEntity> logs = auditLogRepositoryPort.findByEntityTypeAndEntityId("LOCATION", id);

        return logs.stream()
                .map(logEntry -> {
                    String username = "SYSTEM";
                    if (logEntry.getUserId() != null) {
                        username = userRepositoryPort.findById(logEntry.getUserId())
                                .map(u -> u.getUsername())
                                .orElse("UNKNOWN");
                    }
                    List<LocationAuditResponse.AuditDetailResponse> detailResponses = logEntry.getDetails().stream()
                            .map(d -> LocationAuditResponse.AuditDetailResponse.builder()
                                    .fieldName(d.getFieldName())
                                    .oldValue(d.getOldValue())
                                    .newValue(d.getNewValue())
                                    .build())
                            .collect(Collectors.toList());

                    return LocationAuditResponse.builder()
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

    private LocationEntity cloneLocationEntity(LocationEntity source) {
        if (source == null) return null;
        return source.toBuilder().build();
    }

    private void logAuditChange(String username, String action, UUID entityId, LocationEntity before, LocationEntity after) {
        try {
            UserEntity actor = userRepositoryPort.findByUsername(username).orElse(null);
            if (actor != null) {
                Map<String, Object> beforeState = buildAuditState(before);
                Map<String, Object> afterState = buildAuditState(after);
                auditService.log(actor, action, "LOCATION", entityId, beforeState, afterState);
            }
        } catch (Exception e) {
            log.error("Failed to persist audit log for location operation", e);
        }
    }

    private Map<String, Object> buildAuditState(LocationEntity entity) {
        if (entity == null) return null;
        Map<String, Object> state = new HashMap<>();
        state.put("id", entity.getId() != null ? entity.getId().toString() : null);
        state.put("code", entity.getCode());
        state.put("name", entity.getName());
        state.put("zone", entity.getZone());
        state.put("aisle", entity.getAisle());
        state.put("rack", entity.getRack());
        state.put("level", entity.getLevel());
        state.put("position", entity.getPosition());
        state.put("coordX", entity.getCoordX());
        state.put("coordY", entity.getCoordY());
        state.put("coordZ", entity.getCoordZ());
        state.put("type", entity.getType() != null ? entity.getType().name() : null);
        state.put("status", entity.getStatus() != null ? entity.getStatus().name() : null);
        state.put("statusReason", entity.getStatusReason());
        state.put("notes", entity.getNotes());
        state.put("capacityUnits", entity.getCapacityUnits());
        state.put("currentOccupancy", entity.getCurrentOccupancy());
        state.put("branchId", entity.getBranch() != null ? entity.getBranch().getId().toString() : null);
        state.put("sectionId", entity.getSection() != null ? entity.getSection().getId().toString() : null);
        return state;
    }
}
