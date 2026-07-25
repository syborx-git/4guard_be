package com.fourguard.wms.presentation.controller;

import com.fourguard.wms.application.dto.request.CreatePermissionRequest;
import com.fourguard.wms.application.dto.response.PermissionResponse;
import com.fourguard.wms.domain.ports.in.PermissionUseCase;
import com.fourguard.wms.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller para el catálogo de Permisos (RBAC).
 *
 * <p>Context-path {@code /api/v1} es aplicado globalmente en {@code application.yml}.
 * Los permisos son un catálogo semi-estático. No se expone endpoint de actualización
 * porque la tabla {@code wms.permissions} no tiene columnas de auditoría mutables.</p>
 *
 * <ul>
 *   <li>POST   /permissions        — Crear permiso</li>
 *   <li>GET    /permissions        — Listar permisos</li>
 *   <li>GET    /permissions/{id}   — Obtener permiso por ID</li>
 *   <li>DELETE /permissions/{id}   — Eliminar permiso</li>
 * </ul>
 */
@RestController
@RequestMapping("/permissions")
@RequiredArgsConstructor
@Tag(name = "Permisos", description = "Endpoints para la gestión del catálogo de permisos del sistema RBAC")
public class PermissionController {

    private final PermissionUseCase permissionUseCase;

    // ── CREATE ────────────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAuthority('PERMISSIONS_CREATE')")
    @Operation(
        summary     = "Crear permiso",
        description = "Agrega un nuevo permiso al catálogo del sistema. El nombre debe seguir la convención ENTIDAD_ACCION (ej: INVENTORY_READ)."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Permiso creado con éxito"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos inválidos o nombre de permiso duplicado"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes")
    })
    public ResponseEntity<ApiResponse<PermissionResponse>> createPermission(
            @Valid @RequestBody CreatePermissionRequest request) {
        PermissionResponse response = permissionUseCase.createPermission(request);
        return ResponseEntity.ok(ApiResponse.ok("Permiso creado con éxito", response));
    }

    // ── GET ALL ───────────────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAuthority('PERMISSIONS_READ')")
    @Operation(
        summary     = "Listar permisos",
        description = "Recupera el catálogo completo de permisos disponibles en el sistema."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Catálogo de permisos recuperado con éxito"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes")
    })
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> getAllPermissions() {
        List<PermissionResponse> response = permissionUseCase.getAllPermissions();
        return ResponseEntity.ok(ApiResponse.ok("Catálogo de permisos recuperado con éxito", response));
    }

    // ── GET BY ID ─────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISSIONS_READ')")
    @Operation(
        summary     = "Obtener permiso por ID",
        description = "Recupera un permiso específico del catálogo a partir de su UUID."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Permiso encontrado con éxito"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Permiso no encontrado")
    })
    public ResponseEntity<ApiResponse<PermissionResponse>> getPermissionById(
            @Parameter(description = "UUID del permiso", required = true)
            @PathVariable UUID id) {
        PermissionResponse response = permissionUseCase.getPermissionById(id);
        return ResponseEntity.ok(ApiResponse.ok("Permiso encontrado con éxito", response));
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISSIONS_DELETE')")
    @Operation(
        summary     = "Eliminar permiso",
        description = "Elimina un permiso del catálogo. Las asignaciones de este permiso a roles existentes " +
                      "se eliminan automáticamente en cascada (ON DELETE CASCADE en BD)."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Permiso eliminado con éxito"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Permiso no encontrado")
    })
    public ResponseEntity<ApiResponse<Void>> deletePermission(
            @Parameter(description = "UUID del permiso a eliminar", required = true)
            @PathVariable UUID id) {
        permissionUseCase.deletePermission(id);
        return ResponseEntity.ok(ApiResponse.ok("Permiso eliminado con éxito"));
    }

    @GetMapping("/{id}/audit")
    @PreAuthorize("hasAuthority('PERMISSIONS_READ')")
    @Operation(
        summary     = "Obtener historial de auditoría de permiso",
        description = "Recupera la bitácora de cambios para un permiso específico por su UUID."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Historial de auditoría recuperado con éxito"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Permiso no encontrado")
    })
    public ResponseEntity<ApiResponse<List<com.fourguard.wms.application.dto.response.audit.PermissionAuditResponse>>> getPermissionAuditLogs(
            @Parameter(description = "UUID del permiso", required = true)
            @PathVariable UUID id) {
        List<com.fourguard.wms.application.dto.response.audit.PermissionAuditResponse> response = permissionUseCase.getPermissionAuditLogs(id);
        return ResponseEntity.ok(ApiResponse.ok("Historial de auditoría recuperado con éxito", response));
    }
}
