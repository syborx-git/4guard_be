package com.fourguard.wms.presentation.controller;

import com.fourguard.wms.application.dto.request.CreateRoleRequest;
import com.fourguard.wms.application.dto.request.UpdateRoleRequest;
import com.fourguard.wms.application.dto.response.RoleResponse;
import com.fourguard.wms.domain.ports.in.RoleUseCase;
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
import java.util.Set;
import java.util.UUID;

/**
 * REST controller para la gestión de Roles (RBAC).
 *
 * <p>Context-path {@code /api/v1} es aplicado globalmente en {@code application.yml}.
 * Todos los endpoints requieren autenticación JWT y el permiso correspondiente.</p>
 *
 * <ul>
 *   <li>POST   /roles                       — Crear rol</li>
 *   <li>GET    /roles                       — Listar roles</li>
 *   <li>GET    /roles/{id}                  — Obtener rol por ID</li>
 *   <li>PUT    /roles                       — Actualizar rol</li>
 *   <li>DELETE /roles/{id}                  — Eliminar rol</li>
 *   <li>PUT    /roles/{id}/permissions      — Reemplazar permisos de un rol</li>
 * </ul>
 */
@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
@Tag(name = "Roles", description = "Endpoints para la gestión de roles del sistema RBAC")
public class RoleController {

    private final RoleUseCase roleUseCase;

    // ── CREATE ────────────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAuthority('ROLES_CREATE')")
    @Operation(
        summary     = "Crear rol",
        description = "Crea un nuevo rol en el sistema RBAC. Se pueden asignar permisos de forma opcional al crear el rol."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Rol creado con éxito"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos inválidos o nombre de rol duplicado"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes")
    })
    public ResponseEntity<ApiResponse<RoleResponse>> createRole(
            @Valid @RequestBody CreateRoleRequest request) {
        RoleResponse response = roleUseCase.createRole(request);
        return ResponseEntity.ok(ApiResponse.ok("Rol creado con éxito", response));
    }

    // ── GET ALL ───────────────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAuthority('ROLES_READ')")
    @Operation(
        summary     = "Listar roles",
        description = "Recupera la lista completa de roles del sistema."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lista de roles recuperada con éxito"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes")
    })
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getAllRoles() {
        List<RoleResponse> response = roleUseCase.getAllRoles();
        return ResponseEntity.ok(ApiResponse.ok("Lista de roles recuperada con éxito", response));
    }

    // ── GET BY ID ─────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLES_READ')")
    @Operation(
        summary     = "Obtener rol por ID",
        description = "Recupera un rol específico por su UUID, incluyendo todos los permisos asignados."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Rol encontrado con éxito"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Rol no encontrado")
    })
    public ResponseEntity<ApiResponse<RoleResponse>> getRoleById(
            @Parameter(description = "UUID del rol", required = true)
            @PathVariable UUID id) {
        RoleResponse response = roleUseCase.getRoleById(id);
        return ResponseEntity.ok(ApiResponse.ok("Rol encontrado con éxito", response));
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    @PutMapping
    @PreAuthorize("hasAuthority('ROLES_UPDATE')")
    @Operation(
        summary     = "Actualizar rol",
        description = "Actualiza el nombre, nivel o permisos de un rol existente. Si se incluye 'permissionIds', los permisos actuales son reemplazados completamente."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Rol actualizado con éxito"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos inválidos, nombre duplicado, o IDs de permiso incorrectos"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Rol no encontrado")
    })
    public ResponseEntity<ApiResponse<RoleResponse>> updateRole(
            @Valid @RequestBody UpdateRoleRequest request) {
        RoleResponse response = roleUseCase.updateRole(request);
        return ResponseEntity.ok(ApiResponse.ok("Rol actualizado con éxito", response));
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLES_DELETE')")
    @Operation(
        summary     = "Eliminar rol",
        description = "Elimina un rol del sistema. No se puede eliminar un rol de sistema (isSystem=true) ni un rol que tenga usuarios asignados."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Rol eliminado con éxito"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "No se puede eliminar: rol de sistema o con usuarios asignados"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Rol no encontrado")
    })
    public ResponseEntity<ApiResponse<Void>> deleteRole(
            @Parameter(description = "UUID del rol a eliminar", required = true)
            @PathVariable UUID id) {
        roleUseCase.deleteRole(id);
        return ResponseEntity.ok(ApiResponse.ok("Rol eliminado con éxito"));
    }

    // ── ASSIGN PERMISSIONS ────────────────────────────────────────────────────

    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('ROLES_UPDATE')")
    @Operation(
        summary     = "Asignar/reemplazar permisos de un rol",
        description = "Reemplaza completamente el conjunto de permisos del rol con los IDs proporcionados. " +
                      "Para remover todos los permisos, envíe un array vacío []."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Permisos asignados con éxito"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Uno o más IDs de permiso no son válidos"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Rol no encontrado")
    })
    public ResponseEntity<ApiResponse<RoleResponse>> assignPermissions(
            @Parameter(description = "UUID del rol", required = true)
            @PathVariable UUID id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Conjunto de UUIDs de permisos a asignar al rol. Envíe [] para remover todos.",
                required = true
            )
            @RequestBody Set<UUID> permissionIds) {
        RoleResponse response = roleUseCase.assignPermissions(id, permissionIds);
        return ResponseEntity.ok(ApiResponse.ok("Permisos asignados con éxito", response));
    }

    @GetMapping("/{id}/audit")
    @PreAuthorize("hasAuthority('ROLES_READ')")
    @Operation(
        summary     = "Obtener historial de auditoría de rol",
        description = "Recupera la bitácora de cambios y asignaciones de permisos para un rol específico por su UUID."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Historial de auditoría recuperado con éxito"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Rol no encontrado")
    })
    public ResponseEntity<ApiResponse<List<com.fourguard.wms.application.dto.response.audit.RoleAuditResponse>>> getRoleAuditLogs(
            @Parameter(description = "UUID del rol", required = true)
            @PathVariable UUID id) {
        List<com.fourguard.wms.application.dto.response.audit.RoleAuditResponse> response = roleUseCase.getRoleAuditLogs(id);
        return ResponseEntity.ok(ApiResponse.ok("Historial de auditoría recuperado con éxito", response));
    }
}
