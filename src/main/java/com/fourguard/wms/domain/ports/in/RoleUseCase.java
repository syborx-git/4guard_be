package com.fourguard.wms.domain.ports.in;

import com.fourguard.wms.application.dto.request.CreateRoleRequest;
import com.fourguard.wms.application.dto.request.UpdateRoleRequest;
import com.fourguard.wms.application.dto.response.RoleResponse;
import com.fourguard.wms.application.dto.response.audit.RoleAuditResponse;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Port IN — Role CRUD Use Case.
 *
 * <p>Agrupa las operaciones de lectura, escritura y gestión de permisos
 * sobre roles del sistema RBAC.</p>
 */
public interface RoleUseCase {

    /** Crea un nuevo rol, opcionalmente con permisos asignados. */
    RoleResponse createRole(CreateRoleRequest request);

    /** Actualiza los datos de un rol existente. */
    RoleResponse updateRole(UpdateRoleRequest request);

    /** Obtiene un rol por su ID, incluyendo sus permisos. */
    RoleResponse getRoleById(UUID id);

    /** Lista todos los roles del sistema. */
    List<RoleResponse> getAllRoles();

    /** Obtiene el historial de auditoría de un rol por su UUID. */
    List<RoleAuditResponse> getRoleAuditLogs(UUID id);

    /**
     * Elimina un rol. Lanza {@code ValidationException} si el rol
     * tiene usuarios asignados o si es un rol de sistema ({@code isSystem = true}).
     */
    void deleteRole(UUID id);

    /**
     * Reemplaza completamente el conjunto de permisos de un rol.
     * Los permisos no presentes en {@code permissionIds} serán removidos.
     *
     * @param roleId        ID del rol a actualizar
     * @param permissionIds conjunto de IDs de permisos a asignar
     * @return el rol actualizado con sus nuevos permisos
     */
    RoleResponse assignPermissions(UUID roleId, Set<UUID> permissionIds);
}
