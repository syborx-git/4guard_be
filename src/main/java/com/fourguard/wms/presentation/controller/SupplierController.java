package com.fourguard.wms.presentation.controller;

import com.fourguard.wms.application.dto.request.*;
import com.fourguard.wms.application.dto.response.*;
import com.fourguard.wms.domain.ports.in.SupplierUseCase;
import com.fourguard.wms.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for Supplier Management (HU-125).
 * Base path: /suppliers
 */
@RestController
@RequestMapping("/suppliers")
@RequiredArgsConstructor
@Tag(name = "Proveedores", description = "Endpoints para la gestión del catálogo maestro de proveedores (HU-125)")
public class SupplierController {

    private final SupplierUseCase supplierUseCase;

    // =========================================================================
    // POST /suppliers — Create
    // =========================================================================

    @PostMapping
    @PreAuthorize("hasAuthority('SUPPLIERS_CREATE') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Crear proveedor",
               description = "Registra un nuevo proveedor con sus datos fiscales, contacto, dirección y condiciones comerciales. Genera código PRV-XXXX automáticamente.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Proveedor creado con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes")
    })
    public ResponseEntity<ApiResponse<SupplierResponse>> createSupplier(
            @Valid @RequestBody CreateSupplierRequest request) {
        SupplierResponse response = supplierUseCase.createSupplier(request);
        return ResponseEntity.ok(ApiResponse.ok("Proveedor creado con éxito", response));
    }

    // =========================================================================
    // GET /suppliers — List (paginated + filtered)
    // =========================================================================

    @GetMapping
    @PreAuthorize("hasAuthority('SUPPLIERS_READ') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Listar proveedores",
               description = "Recupera la lista paginada de proveedores con filtros dinámicos por status, tipo, scope, cliente, almacén y búsqueda libre.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lista de proveedores recuperada con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes")
    })
    public ResponseEntity<ApiResponse<Page<SupplierSummaryResponse>>> getSuppliers(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String scopeType,
            @RequestParam(required = false) String clientId,
            @RequestParam(required = false) String warehouseId,
            @RequestParam(required = false) Boolean preferredOnly,
            @RequestParam(defaultValue = "0")   int page,
            @RequestParam(defaultValue = "20")  int size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        SupplierFilterRequest filter = SupplierFilterRequest.builder()
                .organizationId(organizationId)
                .search(search)
                .status(status)
                .type(type)
                .scopeType(scopeType)
                .clientId(clientId)
                .warehouseId(warehouseId)
                .preferredOnly(preferredOnly)
                .build();

        Page<SupplierSummaryResponse> response = supplierUseCase.getSuppliers(filter, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Lista de proveedores recuperada con éxito", response));
    }

    // =========================================================================
    // GET /suppliers/{id} — Get by ID
    // =========================================================================

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SUPPLIERS_READ') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Obtener proveedor por ID",
               description = "Recupera el detalle completo de un proveedor incluyendo contacto, dirección y condiciones comerciales.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Proveedor encontrado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Proveedor no encontrado")
    })
    public ResponseEntity<ApiResponse<SupplierResponse>> getSupplierById(@PathVariable UUID id) {
        SupplierResponse response = supplierUseCase.getSupplierById(id);
        return ResponseEntity.ok(ApiResponse.ok("Proveedor encontrado", response));
    }

    // =========================================================================
    // PUT /suppliers/{id} — Update
    // =========================================================================

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SUPPLIERS_UPDATE') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Actualizar proveedor",
               description = "Actualiza todos los datos de un proveedor. Las sub-entidades (contacto, dirección, términos) se actualizan en la misma transacción.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Proveedor actualizado con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Proveedor no encontrado")
    })
    public ResponseEntity<ApiResponse<SupplierResponse>> updateSupplier(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSupplierRequest request) {
        SupplierResponse response = supplierUseCase.updateSupplier(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Proveedor actualizado con éxito", response));
    }

    // =========================================================================
    // PATCH /suppliers/{id}/status — Status Change
    // =========================================================================

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('SUPPLIERS_STATUS_CHANGE') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Cambiar estado de proveedor",
               description = "Cambia el estado operativo del proveedor (ACTIVE, INACTIVE, BLOCKED). El motivo es obligatorio para INACTIVE y BLOCKED.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Estado actualizado con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Estado inválido o motivo ausente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Proveedor no encontrado")
    })
    public ResponseEntity<ApiResponse<SupplierResponse>> updateSupplierStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSupplierStatusRequest request) {
        SupplierResponse response = supplierUseCase.updateSupplierStatus(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Estado del proveedor actualizado con éxito", response));
    }

    // =========================================================================
    // DELETE /suppliers/{id} — Soft Delete (Archive)
    // =========================================================================

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SUPPLIERS_DELETE') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Archivar proveedor",
               description = "Archivado lógico del proveedor (is_deleted=true). No se elimina físicamente. El historial y auditoría se preservan.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Proveedor archivado con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Proveedor no encontrado")
    })
    public ResponseEntity<ApiResponse<Void>> deleteSupplier(@PathVariable UUID id) {
        supplierUseCase.deleteSupplier(id);
        return ResponseEntity.ok(ApiResponse.ok("Proveedor archivado con éxito"));
    }

    // =========================================================================
    // GET /suppliers/{id}/audit — Audit History
    // =========================================================================

    @GetMapping("/{id}/audit")
    @PreAuthorize("hasAuthority('SUPPLIERS_READ') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Historial de auditoría del proveedor",
               description = "Devuelve el historial cronológico de cambios del proveedor, incluyendo cambios de estado, datos y archivado.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Historial recuperado con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Proveedor no encontrado")
    })
    public ResponseEntity<ApiResponse<List<SupplierAuditResponse>>> getSupplierAuditLogs(@PathVariable UUID id) {
        List<SupplierAuditResponse> response = supplierUseCase.getSupplierAuditLogs(id);
        return ResponseEntity.ok(ApiResponse.ok("Historial de auditoría recuperado con éxito", response));
    }

    // =========================================================================
    // GET /suppliers/catalogs/types — Supplier Types Catalog
    // =========================================================================

    @GetMapping("/catalogs/types")
    @PreAuthorize("hasAuthority('SUPPLIERS_READ') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Catálogo de tipos de proveedor",
               description = "Retorna los tipos de proveedor activos de la tabla cat_supplier_types (ordenados por sort_order).")
    public ResponseEntity<ApiResponse<List<SupplierTypeResponse>>> getSupplierTypes() {
        List<SupplierTypeResponse> types = supplierUseCase.getSupplierTypes();
        return ResponseEntity.ok(ApiResponse.ok("Tipos de proveedor recuperados con éxito", types));
    }
}
