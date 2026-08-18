package com.fourguard.wms.presentation.controller;

import com.fourguard.wms.application.dto.request.CreateForkliftOperatorRequest;
import com.fourguard.wms.application.dto.request.UpdateForkliftOperatorRequest;
import com.fourguard.wms.application.dto.request.UpdateForkliftOperatorStatusRequest;
import com.fourguard.wms.application.dto.response.ForkliftOperatorResponse;
import com.fourguard.wms.application.dto.response.audit.ForkliftOperatorAuditResponse;
import com.fourguard.wms.domain.exception.ValidationException;
import com.fourguard.wms.domain.ports.in.ForkliftOperatorUseCase;
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

/**
 * REST Controller for Forklift Operator Catalog Management (HU-142).
 * Base path: /api/v1/forklift-operators
 */
@RestController
@RequestMapping("/forklift-operators")
@RequiredArgsConstructor
@Tag(name = "Montacarguistas", description = "Endpoints para la gestión del catálogo de operadores de montacargas certificados (HU-142)")
public class ForkliftOperatorController {

    private final ForkliftOperatorUseCase forkliftOperatorUseCase;

    // ─── CREATE ─────────────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAuthority('FORKLIFT_OPERATORS_CREATE') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Registrar montacarguista",
               description = "Crea un nuevo operador en el catálogo maestro. El código (MC-XXX) se genera automáticamente.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Montacarguista registrado con éxito"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o DC-3 duplicado"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes")
    })
    public ResponseEntity<ApiResponse<ForkliftOperatorResponse>> createOperator(
            @Valid @RequestBody CreateForkliftOperatorRequest request) {
        ForkliftOperatorResponse response = forkliftOperatorUseCase.createOperator(request);
        return ResponseEntity.ok(ApiResponse.ok("Montacarguista registrado con éxito", response));
    }

    // ─── UPDATE ─────────────────────────────────────────────────────────────────

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('FORKLIFT_OPERATORS_UPDATE') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Actualizar montacarguista",
               description = "Actualiza los datos de un operador existente. El código operativo es inmutable.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Montacarguista actualizado con éxito"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos inválidos o DC-3 duplicado"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Montacarguista no encontrado"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflicto de versión (optimistic lock)")
    })
    public ResponseEntity<ApiResponse<ForkliftOperatorResponse>> updateOperator(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateForkliftOperatorRequest request) {
        if (!id.equals(request.getId())) {
            throw new ValidationException("El ID del path no coincide con el ID del cuerpo de la solicitud.");
        }
        ForkliftOperatorResponse response = forkliftOperatorUseCase.updateOperator(request);
        return ResponseEntity.ok(ApiResponse.ok("Montacarguista actualizado con éxito", response));
    }

    // ─── GET BY ID ───────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FORKLIFT_OPERATORS_READ') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER') or hasRole('WAREHOUSE_SUPERVISOR')")
    @Operation(summary = "Obtener montacarguista por ID",
               description = "Retorna el detalle completo de un operador activo en el catálogo.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Montacarguista encontrado"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Montacarguista no encontrado")
    })
    public ResponseEntity<ApiResponse<ForkliftOperatorResponse>> getOperatorById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(null, forkliftOperatorUseCase.getOperatorById(id)));
    }

    // ─── LIST ────────────────────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAuthority('FORKLIFT_OPERATORS_READ') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER') or hasRole('WAREHOUSE_SUPERVISOR') or hasRole('SHIFT_LEADER')")
    @Operation(summary = "Listar montacarguistas",
               description = "Retorna la lista de montacarguistas con filtros opcionales por organización, sucursal, estatus, vigencia de licencia y búsqueda de texto libre.")
    public ResponseEntity<ApiResponse<List<ForkliftOperatorResponse>>> getOperators(
            @RequestParam UUID organizationId,
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String licenseStatus,
            @RequestParam(required = false) String search) {

        List<ForkliftOperatorResponse> operators = forkliftOperatorUseCase.getOperators(
                organizationId, branchId, status, licenseStatus, search);
        return ResponseEntity.ok(ApiResponse.ok(null, operators));
    }

    // ─── DELETE ─────────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('FORKLIFT_OPERATORS_DELETE') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Eliminar montacarguista",
               description = "Realiza una baja lógica (soft delete) del operador. El registro permanece en BD para auditoría.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Montacarguista eliminado con éxito"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Montacarguista no encontrado")
    })
    public ResponseEntity<ApiResponse<Void>> deleteOperator(@PathVariable UUID id) {
        forkliftOperatorUseCase.deleteOperator(id);
        return ResponseEntity.ok(ApiResponse.ok("Montacarguista eliminado con éxito", null));
    }

    // ─── STATUS CHANGE ───────────────────────────────────────────────────────────

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('FORKLIFT_OPERATORS_STATUS_CHANGE') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER') or hasRole('WAREHOUSE_SUPERVISOR')")
    @Operation(summary = "Cambiar estatus de montacarguista",
               description = "Alterna el estatus entre ACTIVO e INACTIVO, con registro de motivo en auditoría.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Estatus actualizado con éxito"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Estatus inválido"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Montacarguista no encontrado")
    })
    public ResponseEntity<ApiResponse<ForkliftOperatorResponse>> updateOperatorStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateForkliftOperatorStatusRequest request) {
        ForkliftOperatorResponse response = forkliftOperatorUseCase.updateOperatorStatus(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Estatus actualizado con éxito", response));
    }

    // ─── AUDIT HISTORY ───────────────────────────────────────────────────────────

    @GetMapping("/{id}/audit")
    @PreAuthorize("hasAuthority('FORKLIFT_OPERATORS_READ') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Historial de auditoría",
               description = "Retorna la bitácora de todos los cambios realizados sobre un montacarguista con deltas por campo.")
    public ResponseEntity<ApiResponse<List<ForkliftOperatorAuditResponse>>> getOperatorAuditLogs(@PathVariable UUID id) {
        List<ForkliftOperatorAuditResponse> logs = forkliftOperatorUseCase.getOperatorAuditLogs(id);
        return ResponseEntity.ok(ApiResponse.ok(null, logs));
    }
}
