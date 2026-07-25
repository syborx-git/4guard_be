package com.fourguard.wms.application.usecase;

import com.fourguard.wms.application.dto.request.CreatePermissionRequest;
import com.fourguard.wms.application.dto.response.PermissionResponse;
import com.fourguard.wms.application.dto.response.audit.PermissionAuditResponse;
import com.fourguard.wms.application.mapper.PermissionMapper;
import com.fourguard.wms.domain.exception.EntityNotFoundException;
import com.fourguard.wms.domain.exception.ValidationException;
import com.fourguard.wms.domain.ports.in.PermissionUseCase;
import com.fourguard.wms.domain.ports.out.AuditLogRepositoryPort;
import com.fourguard.wms.domain.ports.out.PermissionRepositoryPort;
import com.fourguard.wms.domain.ports.out.UserRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.AuditLogEntity;
import com.fourguard.wms.infrastructure.persistence.entity.PermissionEntity;
import com.fourguard.wms.infrastructure.persistence.entity.UserEntity;
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

/**
 * Implementación del use case de Permisos.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionService implements PermissionUseCase {

    private final PermissionRepositoryPort permissionRepositoryPort;
    private final PermissionMapper         permissionMapper;
    private final SecurityAuditHelper      securityAuditHelper;
    private final AuditService             auditService;
    private final AuditLogRepositoryPort   auditLogRepositoryPort;
    private final UserRepositoryPort       userRepositoryPort;

    // ── CREATE ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public PermissionResponse createPermission(CreatePermissionRequest request) {
        log.info("Creating permission with name: {}", request.getName());

        if (permissionRepositoryPort.existsByName(request.getName())) {
            throw new ValidationException("Ya existe un permiso con el nombre: " + request.getName());
        }

        PermissionEntity entity = permissionMapper.toEntity(request);
        PermissionEntity saved  = permissionRepositoryPort.save(entity);

        String currentUser = securityAuditHelper.getCurrentUsername();
        Map<String, Object> afterState = buildAuditState(saved);
        logAuditChange(currentUser, "PERMISSION_CREATED", saved.getId(), null, afterState);

        log.info("Permission created successfully with ID: {}", saved.getId());
        return permissionMapper.toResponse(saved);
    }

    // ── GET BY ID ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PermissionResponse getPermissionById(UUID id) {
        log.debug("Fetching permission with ID: {}", id);
        PermissionEntity entity = permissionRepositoryPort.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Permiso no encontrado con ID: " + id));
        return permissionMapper.toResponse(entity);
    }

    // ── GET ALL ───────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<PermissionResponse> getAllPermissions() {
        log.debug("Fetching all permissions");
        return permissionMapper.toResponseList(permissionRepositoryPort.findAll());
    }

    // ── AUDIT LOGS ────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<PermissionAuditResponse> getPermissionAuditLogs(UUID id) {
        log.debug("Fetching audit logs for permission: {}", id);
        if (!permissionRepositoryPort.findById(id).isPresent()) {
            throw new EntityNotFoundException("Permiso no encontrado con ID: " + id);
        }

        List<AuditLogEntity> logs = auditLogRepositoryPort.findByEntityTypeAndEntityId("PERMISSION", id);

        return logs.stream()
                .map(logEntry -> {
                    String username = "SYSTEM";
                    if (logEntry.getUserId() != null) {
                        username = userRepositoryPort.findById(logEntry.getUserId())
                                .map(UserEntity::getUsername)
                                .orElse("UNKNOWN");
                    }
                    List<PermissionAuditResponse.AuditDetailResponse> detailResponses = logEntry.getDetails().stream()
                            .map(d -> PermissionAuditResponse.AuditDetailResponse.builder()
                                    .fieldName(d.getFieldName())
                                    .oldValue(d.getOldValue())
                                    .newValue(d.getNewValue())
                                    .build())
                            .collect(Collectors.toList());

                    return PermissionAuditResponse.builder()
                            .logId(logEntry.getLogId())
                            .action(logEntry.getAction())
                            .username(username)
                            .createdAt(logEntry.getCreatedAt())
                            .details(detailResponses)
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void deletePermission(UUID id) {
        log.info("Deleting permission with ID: {}", id);

        PermissionEntity existing = permissionRepositoryPort.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Permiso no encontrado con ID: " + id));

        Map<String, Object> beforeState = buildAuditState(existing);
        String currentUser = securityAuditHelper.getCurrentUsername();

        permissionRepositoryPort.deleteById(id);

        // Audit log
        logAuditChange(currentUser, "PERMISSION_DELETED", id, beforeState, null);

        log.info("Permission deleted successfully with ID: {}", id);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Map<String, Object> buildAuditState(PermissionEntity entity) {
        if (entity == null) return null;
        Map<String, Object> state = new HashMap<>();
        state.put("name", entity.getName());
        state.put("description", entity.getDescription());
        return state;
    }

    private void logAuditChange(String username, String action, UUID entityId, Map<String, Object> beforeState, Map<String, Object> afterState) {
        try {
            UserEntity actor = userRepositoryPort.findByUsername(username).orElse(null);
            if (actor != null) {
                auditService.log(actor, action, "PERMISSION", entityId, beforeState, afterState);
            }
        } catch (Exception e) {
            log.error("Failed to persist audit log for permission operation", e);
        }
    }
}
