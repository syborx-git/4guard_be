package com.fourguard.wms.application.usecase;

import com.fourguard.wms.application.dto.request.CreateRoleRequest;
import com.fourguard.wms.application.dto.request.UpdateRoleRequest;
import com.fourguard.wms.application.dto.response.RoleResponse;
import com.fourguard.wms.application.dto.response.audit.RoleAuditResponse;
import com.fourguard.wms.application.mapper.RoleMapper;
import com.fourguard.wms.domain.exception.EntityNotFoundException;
import com.fourguard.wms.domain.exception.ValidationException;
import com.fourguard.wms.domain.ports.in.RoleUseCase;
import com.fourguard.wms.domain.ports.out.AuditLogRepositoryPort;
import com.fourguard.wms.domain.ports.out.PermissionRepositoryPort;
import com.fourguard.wms.domain.ports.out.RoleRepositoryPort;
import com.fourguard.wms.domain.ports.out.UserRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.AuditLogEntity;
import com.fourguard.wms.infrastructure.persistence.entity.PermissionEntity;
import com.fourguard.wms.infrastructure.persistence.entity.RoleEntity;
import com.fourguard.wms.infrastructure.persistence.entity.UserEntity;
import com.fourguard.wms.shared.audit.AuditService;
import com.fourguard.wms.shared.audit.SecurityAuditHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementación del use case de Roles.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoleService implements RoleUseCase {

    private final RoleRepositoryPort       roleRepositoryPort;
    private final PermissionRepositoryPort permissionRepositoryPort;
    private final RoleMapper               roleMapper;
    private final SecurityAuditHelper      securityAuditHelper;
    private final AuditService             auditService;
    private final AuditLogRepositoryPort   auditLogRepositoryPort;
    private final UserRepositoryPort       userRepositoryPort;

    // ── CREATE ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public RoleResponse createRole(CreateRoleRequest request) {
        log.info("Creating role with name: {}", request.getName());

        validateUniqueRoleName(request.getName(), null);

        RoleEntity entity = roleMapper.toEntity(request);
        String currentUser = securityAuditHelper.getCurrentUsername();
        entity.setCreatedBy(currentUser);

        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            Set<PermissionEntity> permissions = resolvePermissions(request.getPermissionIds());
            entity.setPermissions(permissions);
        }

        RoleEntity saved = roleRepositoryPort.save(entity);

        // Audit log
        Map<String, Object> afterState = buildAuditState(saved);
        logAuditChange(currentUser, "ROLE_CREATED", saved.getId(), null, afterState);

        log.info("Role created successfully with ID: {}", saved.getId());
        return roleMapper.toResponse(saved);
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public RoleResponse updateRole(UpdateRoleRequest request) {
        log.info("Updating role with ID: {}", request.getId());

        RoleEntity existing = findRoleOrThrow(request.getId());
        validateUniqueRoleName(request.getName(), existing.getId());

        Map<String, Object> beforeState = buildAuditState(existing);

        roleMapper.updateEntityFromDto(request, existing);
        String currentUser = securityAuditHelper.getCurrentUsername();
        existing.setUpdatedBy(currentUser);

        if (request.getPermissionIds() != null) {
            Set<PermissionEntity> permissions = resolvePermissions(request.getPermissionIds());
            existing.getPermissions().clear();
            existing.getPermissions().addAll(permissions);
        }

        RoleEntity saved = roleRepositoryPort.save(existing);
        Map<String, Object> afterState = buildAuditState(saved);

        // Audit log
        logAuditChange(currentUser, "ROLE_UPDATED", saved.getId(), beforeState, afterState);

        log.info("Role updated successfully with ID: {}", saved.getId());
        return roleMapper.toResponse(saved);
    }

    // ── GET BY ID ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public RoleResponse getRoleById(UUID id) {
        log.debug("Fetching role with ID: {}", id);
        RoleEntity entity = roleRepositoryPort.findByIdWithPermissions(id)
                .orElseThrow(() -> new EntityNotFoundException("Rol no encontrado con ID: " + id));
        return roleMapper.toResponse(entity);
    }

    // ── GET ALL ───────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles() {
        log.debug("Fetching all roles");
        return roleMapper.toResponseList(roleRepositoryPort.findAll());
    }

    // ── AUDIT LOGS ────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<RoleAuditResponse> getRoleAuditLogs(UUID id) {
        log.debug("Fetching audit logs for role: {}", id);
        if (!roleRepositoryPort.findById(id).isPresent()) {
            throw new EntityNotFoundException("Rol no encontrado con ID: " + id);
        }

        List<AuditLogEntity> logs = auditLogRepositoryPort.findByEntityTypeAndEntityId("ROLE", id);

        return logs.stream()
                .map(logEntry -> {
                    String username = "SYSTEM";
                    if (logEntry.getUserId() != null) {
                        username = userRepositoryPort.findById(logEntry.getUserId())
                                .map(UserEntity::getUsername)
                                .orElse("UNKNOWN");
                    }
                    List<RoleAuditResponse.AuditDetailResponse> detailResponses = logEntry.getDetails().stream()
                            .map(d -> RoleAuditResponse.AuditDetailResponse.builder()
                                    .fieldName(d.getFieldName())
                                    .oldValue(d.getOldValue())
                                    .newValue(d.getNewValue())
                                    .build())
                            .collect(Collectors.toList());

                    return RoleAuditResponse.builder()
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
    public void deleteRole(UUID id) {
        log.info("Deleting role with ID: {}", id);

        RoleEntity existing = findRoleOrThrow(id);

        if (Boolean.TRUE.equals(existing.getIsSystem())) {
            throw new ValidationException("No se puede eliminar un rol de sistema: " + existing.getName());
        }

        if (roleRepositoryPort.existsUserAssignedToRole(id)) {
            throw new ValidationException(
                "No se puede eliminar el rol '" + existing.getName() +
                "' porque tiene usuarios asignados. Reasigne los usuarios antes de eliminarlo."
            );
        }

        Map<String, Object> beforeState = buildAuditState(existing);
        String currentUser = securityAuditHelper.getCurrentUsername();

        roleRepositoryPort.deleteById(id);

        // Audit log
        logAuditChange(currentUser, "ROLE_DELETED", id, beforeState, null);

        log.info("Role deleted successfully with ID: {}", id);
    }

    // ── ASSIGN PERMISSIONS ────────────────────────────────────────────────────

    @Override
    @Transactional
    public RoleResponse assignPermissions(UUID roleId, Set<UUID> permissionIds) {
        log.info("Assigning {} permissions to role ID: {}", permissionIds.size(), roleId);

        RoleEntity existing = roleRepositoryPort.findByIdWithPermissions(roleId)
                .orElseThrow(() -> new EntityNotFoundException("Rol no encontrado con ID: " + roleId));

        Map<String, Object> beforeState = buildAuditState(existing);

        Set<PermissionEntity> permissions = permissionIds.isEmpty()
                ? Collections.emptySet()
                : resolvePermissions(permissionIds);

        existing.getPermissions().clear();
        existing.getPermissions().addAll(permissions);
        String currentUser = securityAuditHelper.getCurrentUsername();
        existing.setUpdatedBy(currentUser);

        RoleEntity saved = roleRepositoryPort.save(existing);
        Map<String, Object> afterState = buildAuditState(saved);

        // Audit log
        logAuditChange(currentUser, "ROLE_PERMISSIONS_ASSIGNED", roleId, beforeState, afterState);

        log.info("Permissions assigned successfully to role ID: {}", roleId);
        return roleMapper.toResponse(saved);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private RoleEntity findRoleOrThrow(UUID id) {
        return roleRepositoryPort.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Rol no encontrado con ID: " + id));
    }

    private void validateUniqueRoleName(String name, UUID excludeId) {
        roleRepositoryPort.findByName(name).ifPresent(existing -> {
            if (!existing.getId().equals(excludeId)) {
                throw new ValidationException("Ya existe un rol con el nombre: " + name);
            }
        });
    }

    private Set<PermissionEntity> resolvePermissions(Set<UUID> ids) {
        Set<PermissionEntity> found = permissionRepositoryPort.findAllByIds(ids);
        if (found.size() != ids.size()) {
            throw new ValidationException(
                "Uno o más IDs de permisos no son válidos. Verifique los IDs enviados."
            );
        }
        return found;
    }

    private Map<String, Object> buildAuditState(RoleEntity entity) {
        if (entity == null) return null;
        Map<String, Object> state = new HashMap<>();
        state.put("name", entity.getName());
        state.put("level", entity.getLevel());
        state.put("isSystem", entity.getIsSystem());
        if (entity.getPermissions() != null) {
            state.put("permissionsCount", entity.getPermissions().size());
            List<String> permNames = entity.getPermissions().stream()
                    .map(PermissionEntity::getName)
                    .sorted()
                    .collect(Collectors.toList());
            state.put("permissions", String.join(", ", permNames));
        } else {
            state.put("permissionsCount", 0);
        }
        return state;
    }

    private void logAuditChange(String username, String action, UUID entityId, Map<String, Object> beforeState, Map<String, Object> afterState) {
        try {
            UserEntity actor = userRepositoryPort.findByUsername(username).orElse(null);
            if (actor != null) {
                auditService.log(actor, action, "ROLE", entityId, beforeState, afterState);
            }
        } catch (Exception e) {
            log.error("Failed to persist audit log for role operation", e);
        }
    }
}
