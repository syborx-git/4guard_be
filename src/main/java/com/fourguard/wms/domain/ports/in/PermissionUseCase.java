package com.fourguard.wms.domain.ports.in;

import com.fourguard.wms.application.dto.request.CreatePermissionRequest;
import com.fourguard.wms.application.dto.response.PermissionResponse;
import com.fourguard.wms.application.dto.response.audit.PermissionAuditResponse;

import java.util.List;
import java.util.UUID;

/**
 * Port IN — Permission CRUD Use Case.
 *
 * <p>Los permisos son un catálogo semi-estático. La tabla {@code permissions}
 * solo tiene {@code created_at} (no version / updated_at), por lo que
 * no se expone operación de actualización.</p>
 */
public interface PermissionUseCase {

    /** Crea un nuevo permiso en el catálogo. */
    PermissionResponse createPermission(CreatePermissionRequest request);

    /** Obtiene un permiso por su ID. */
    PermissionResponse getPermissionById(UUID id);

    /** Lista todos los permisos disponibles en el sistema. */
    List<PermissionResponse> getAllPermissions();

    /** Obtiene el historial de auditoría de un permiso por su UUID. */
    List<PermissionAuditResponse> getPermissionAuditLogs(UUID id);

    /**
     * Elimina un permiso del catálogo.
     * Las filas en {@code role_permissions} se eliminarán en cascada (ON DELETE CASCADE en DB).
     */
    void deletePermission(UUID id);
}
