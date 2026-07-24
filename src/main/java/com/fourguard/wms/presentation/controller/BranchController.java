package com.fourguard.wms.presentation.controller;

import com.fourguard.wms.application.dto.request.CreateBranchRequest;
import com.fourguard.wms.application.dto.request.UpdateBranchRequest;
import com.fourguard.wms.application.dto.response.BranchResponse;
import com.fourguard.wms.domain.ports.in.CreateBranchUseCase;
import com.fourguard.wms.domain.ports.in.DeleteBranchUseCase;
import com.fourguard.wms.domain.ports.in.GetBranchUseCase;
import com.fourguard.wms.domain.ports.in.UpdateBranchUseCase;
import com.fourguard.wms.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** REST controller for Branch Management. */
@RestController
@RequestMapping("/branches")
@RequiredArgsConstructor
@Tag(name = "Sucursales", description = "Endpoints para la gestión y administración de sucursales (almacenes)")
public class BranchController {

    private final CreateBranchUseCase createBranchUseCase;
    private final UpdateBranchUseCase updateBranchUseCase;
    private final GetBranchUseCase getBranchUseCase;
    private final DeleteBranchUseCase deleteBranchUseCase;

    @PostMapping
    @PreAuthorize("hasAuthority('BRANCHES_CREATE') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Crear sucursal", description = "Crea una nueva sucursal en el sistema.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Sucursal creada con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes")
    })
    public ResponseEntity<ApiResponse<BranchResponse>> createBranch(@Valid @RequestBody CreateBranchRequest request) {
        BranchResponse response = createBranchUseCase.createBranch(request);
        return ResponseEntity.ok(ApiResponse.ok("Sucursal creada con éxito", response));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('BRANCHES_UPDATE') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Actualizar sucursal", description = "Actualiza los datos de una sucursal existente.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Sucursal actualizada con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Sucursal no encontrada")
    })
    public ResponseEntity<ApiResponse<BranchResponse>> updateBranch(@Valid @RequestBody UpdateBranchRequest request) {
        BranchResponse response = updateBranchUseCase.updateBranch(request);
        return ResponseEntity.ok(ApiResponse.ok("Sucursal actualizada con éxito", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('BRANCHES_READ') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Obtener sucursal por ID", description = "Recupera los detalles de una sucursal específica por su UUID.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Sucursal encontrada con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Sucursal no encontrada")
    })
    public ResponseEntity<ApiResponse<BranchResponse>> getBranchById(@PathVariable UUID id) {
        BranchResponse response = getBranchUseCase.getBranchById(id);
        return ResponseEntity.ok(ApiResponse.ok("Sucursal encontrada con éxito", response));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('BRANCHES_READ') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Obtener sucursales", description = "Recupera la lista de sucursales, opcionalmente filtrada por organización.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lista de sucursales recuperada con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes")
    })
    public ResponseEntity<ApiResponse<List<BranchResponse>>> getBranches(@RequestParam(required = false) UUID organizationId) {
        List<BranchResponse> response;
        if (organizationId != null) {
            response = getBranchUseCase.getBranchesByOrganizationId(organizationId);
        } else {
            response = getBranchUseCase.getAllBranches();
        }
        return ResponseEntity.ok(ApiResponse.ok("Lista de sucursales recuperada con éxito", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('BRANCHES_DELETE') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Eliminar sucursal", description = "Elimina físicamente una sucursal del sistema por su ID.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Sucursal eliminada con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Sucursal no encontrada")
    })
    public ResponseEntity<ApiResponse<Void>> deleteBranch(@PathVariable UUID id) {
        deleteBranchUseCase.deleteBranch(id);
        return ResponseEntity.ok(ApiResponse.ok("Sucursal eliminada con éxito"));
    }

    @GetMapping("/{id}/audit")
    @PreAuthorize("hasAuthority('BRANCHES_READ') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Obtener historial de auditoría de sucursal", description = "Recupera la bitácora de cambios para una sucursal específica por su UUID.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Historial de auditoría recuperado con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Sucursal no encontrada")
    })
    public ResponseEntity<ApiResponse<List<com.fourguard.wms.application.dto.response.audit.BranchAuditResponse>>> getBranchAuditLogs(@PathVariable UUID id) {
        List<com.fourguard.wms.application.dto.response.audit.BranchAuditResponse> response = getBranchUseCase.getBranchAuditLogs(id);
        return ResponseEntity.ok(ApiResponse.ok("Historial de auditoría recuperado con éxito", response));
    }
}
