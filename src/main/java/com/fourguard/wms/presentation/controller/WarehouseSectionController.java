package com.fourguard.wms.presentation.controller;

import com.fourguard.wms.application.dto.request.CreateWarehouseSectionRequest;
import com.fourguard.wms.application.dto.request.InitializeWarehouseSectionRequest;
import com.fourguard.wms.application.dto.request.UpdateWarehouseSectionRequest;
import com.fourguard.wms.application.dto.request.UpdateWarehouseSectionStatusRequest;
import com.fourguard.wms.application.dto.response.WarehouseSectionResponse;
import com.fourguard.wms.application.dto.response.audit.WarehouseSectionAuditResponse;
import com.fourguard.wms.domain.ports.in.WarehouseSectionUseCase;
import com.fourguard.wms.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** REST controller for Warehouse Section Management. */
@RestController
@RequestMapping("/warehouse-sections")
@RequiredArgsConstructor
@Tag(name = "Secciones de Almacén", description = "Endpoints para la gestión y administración de las áreas/secciones de una sucursal")
public class WarehouseSectionController {

    private final WarehouseSectionUseCase sectionUseCase;

    @PostMapping
    @PreAuthorize("hasAuthority('SECTIONS_CREATE') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Crear sección de almacén", description = "Crea una nueva sección física o lógica dentro de una sucursal.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Sección creada con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes")
    })
    public ResponseEntity<ApiResponse<WarehouseSectionResponse>> createSection(@Valid @RequestBody CreateWarehouseSectionRequest request) {
        WarehouseSectionResponse response = sectionUseCase.createWarehouseSection(request);
        return ResponseEntity.ok(ApiResponse.ok("Sección de almacén creada con éxito", response));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('SECTIONS_UPDATE') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Actualizar sección de almacén", description = "Actualiza los datos de una sección existente.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Sección actualizada con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Sección no encontrada")
    })
    public ResponseEntity<ApiResponse<WarehouseSectionResponse>> updateSection(@Valid @RequestBody UpdateWarehouseSectionRequest request) {
        WarehouseSectionResponse response = sectionUseCase.updateWarehouseSection(request);
        return ResponseEntity.ok(ApiResponse.ok("Sección de almacén actualizada con éxito", response));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('SECTIONS_UPDATE') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Cambiar estado de la sección", description = "Cambia el estado operativo de una sección de almacén (ACTIVE, INACTIVE).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Estado de la sección actualizado con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Sección no encontrada")
    })
    public ResponseEntity<ApiResponse<WarehouseSectionResponse>> updateSectionStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateWarehouseSectionStatusRequest request) {
        WarehouseSectionResponse response = sectionUseCase.updateWarehouseSectionStatus(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Estado de la sección actualizado con éxito", response));
    }

    @PostMapping("/{id}/initialize")
    @PreAuthorize("hasAuthority('SECTIONS_UPDATE') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Inicializar y activar nave de almacén", description = "Configura métricas de estiba, categoría y genera cuadrícula de ubicaciones operativas.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Nave inicializada y activada con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Sección no encontrada")
    })
    public ResponseEntity<ApiResponse<WarehouseSectionResponse>> initializeSection(
            @PathVariable UUID id,
            @Valid @RequestBody InitializeWarehouseSectionRequest request,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "SYSTEM";
        WarehouseSectionResponse response = sectionUseCase.initializeWarehouseSection(id, request, username);
        return ResponseEntity.ok(ApiResponse.ok("Nave de almacén inicializada y activada con éxito", response));
    }


    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SECTIONS_READ') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Obtener sección por ID", description = "Recupera los detalles de una sección específica por su UUID.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Sección encontrada con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Sección no encontrada")
    })
    public ResponseEntity<ApiResponse<WarehouseSectionResponse>> getSectionById(@PathVariable UUID id) {
        WarehouseSectionResponse response = sectionUseCase.getWarehouseSectionById(id);
        return ResponseEntity.ok(ApiResponse.ok("Sección encontrada con éxito", response));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SECTIONS_READ') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Obtener secciones", description = "Recupera la lista de secciones, opcionalmente filtrada por sucursal.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lista de secciones recuperada con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes")
    })
    public ResponseEntity<ApiResponse<List<WarehouseSectionResponse>>> getSections(@RequestParam(required = false) UUID branchId) {
        List<WarehouseSectionResponse> response;
        if (branchId != null) {
            response = sectionUseCase.getWarehouseSectionsByBranchId(branchId);
        } else {
            response = sectionUseCase.getAllWarehouseSections();
        }
        return ResponseEntity.ok(ApiResponse.ok("Lista de secciones recuperada con éxito", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SECTIONS_DELETE') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Eliminar sección de almacén", description = "Elimina físicamente una sección del sistema por su ID.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Sección eliminada con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Sección no encontrada")
    })
    public ResponseEntity<ApiResponse<Void>> deleteSection(@PathVariable UUID id) {
        sectionUseCase.deleteWarehouseSection(id);
        return ResponseEntity.ok(ApiResponse.ok("Sección eliminada con éxito"));
    }

    @GetMapping("/{id}/audit")
    @PreAuthorize("hasAuthority('SECTIONS_READ') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Historial de auditoría de la sección", description = "Devuelve el historial cronológico de cambios de una sección de almacén.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Historial recuperado con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Sección no encontrada")
    })
    public ResponseEntity<ApiResponse<List<WarehouseSectionAuditResponse>>> getSectionAuditLogs(@PathVariable UUID id) {
        List<WarehouseSectionAuditResponse> response = sectionUseCase.getWarehouseSectionAuditLogs(id);
        return ResponseEntity.ok(ApiResponse.ok("Historial de auditoría recuperado con éxito", response));
    }
}
