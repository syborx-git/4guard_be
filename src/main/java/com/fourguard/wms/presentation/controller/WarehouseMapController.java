package com.fourguard.wms.presentation.controller;

import com.fourguard.wms.application.dto.request.map.UpdatePositionStatusMapRequest;
import com.fourguard.wms.application.dto.response.map.CatBlockReasonResponse;
import com.fourguard.wms.application.dto.response.map.PositionMapDetailResponse;
import com.fourguard.wms.application.dto.response.map.WarehouseTopologyResponse;
import com.fourguard.wms.domain.ports.in.WarehouseMapUseCase;
import com.fourguard.wms.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/warehouse-map")
@RequiredArgsConstructor
@Tag(name = "Mapa de Nave 2D", description = "Endpoints de topología espacial, blueprint SVG y gestión de posiciones en tiempo real")
public class WarehouseMapController {

    private final WarehouseMapUseCase mapUseCase;

    @GetMapping("/topology")
    @PreAuthorize("hasAuthority('INVENTORY_READ') or hasAuthority('LAYOUT_READ') or hasAuthority('SECTIONS_READ') or hasAuthority('LOCATIONS_READ') or hasAuthority('RECEIVING_READ') or hasAuthority('WAREHOUSE_MOVEMENTS_READ') or hasAuthority('QUALITY_READ') or hasRole('OPERATIONS_MANAGER') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_SUPERVISOR') or hasRole('WAREHOUSE_SUPERVISOR') or hasRole('SHIFT_LEADER') or hasRole('WAREHOUSE_OPERATOR') or hasRole('SECURITY_GUARD') or hasRole('VIGILANCIA') or hasRole('FORKLIFT_OPERATOR') or hasRole('CONTROL_DESK') or hasRole('CEO')")
    @Operation(summary = "Obtener topología 2D completa", description = "Retorna las naves del almacén con coordenadas SVG y métricas agregadas de ocupación.")
    public ResponseEntity<ApiResponse<WarehouseTopologyResponse>> getTopology(
            @RequestParam UUID branchId) {
        WarehouseTopologyResponse response = mapUseCase.getTopology(branchId);
        return ResponseEntity.ok(ApiResponse.ok("Topología del almacén recuperada con éxito", response));
    }

    @GetMapping("/sections/{sectionId}/positions")
    @PreAuthorize("hasAuthority('INVENTORY_READ') or hasAuthority('LAYOUT_READ') or hasAuthority('SECTIONS_READ') or hasAuthority('LOCATIONS_READ') or hasAuthority('RECEIVING_READ') or hasAuthority('WAREHOUSE_MOVEMENTS_READ') or hasAuthority('QUALITY_READ') or hasRole('OPERATIONS_MANAGER') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_SUPERVISOR') or hasRole('WAREHOUSE_SUPERVISOR') or hasRole('SHIFT_LEADER') or hasRole('WAREHOUSE_OPERATOR') or hasRole('SECURITY_GUARD') or hasRole('VIGILANCIA') or hasRole('FORKLIFT_OPERATOR') or hasRole('CONTROL_DESK') or hasRole('CEO')")
    @Operation(summary = "Consultar posiciones de una sección", description = "Retorna la cuadrícula de posiciones de una nave con filtros de estado y búsqueda.")
    public ResponseEntity<ApiResponse<List<PositionMapDetailResponse>>> getPositionsBySection(
            @PathVariable UUID sectionId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        List<PositionMapDetailResponse> response = mapUseCase.getPositionsBySection(sectionId, status, search);
        return ResponseEntity.ok(ApiResponse.ok("Posiciones recuperadas con éxito", response));
    }

    @GetMapping("/positions")
    @PreAuthorize("hasAuthority('INVENTORY_READ') or hasAuthority('LAYOUT_READ') or hasAuthority('SECTIONS_READ') or hasAuthority('LOCATIONS_READ') or hasAuthority('RECEIVING_READ') or hasAuthority('WAREHOUSE_MOVEMENTS_READ') or hasAuthority('QUALITY_READ') or hasRole('OPERATIONS_MANAGER') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_SUPERVISOR') or hasRole('WAREHOUSE_SUPERVISOR') or hasRole('SHIFT_LEADER') or hasRole('WAREHOUSE_OPERATOR') or hasRole('SECURITY_GUARD') or hasRole('VIGILANCIA') or hasRole('FORKLIFT_OPERATOR') or hasRole('CONTROL_DESK') or hasRole('CEO')")
    @Operation(summary = "Consultar todas las posiciones/bahías", description = "Retorna la cuadrícula completa de posiciones de la sucursal o filtrada por sección para la tabla de Consulta de Bahías.")
    public ResponseEntity<ApiResponse<List<PositionMapDetailResponse>>> getAllPositions(
            @RequestParam UUID branchId,
            @RequestParam(required = false) UUID sectionId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        List<PositionMapDetailResponse> response = mapUseCase.getAllPositions(branchId, sectionId, status, search);
        return ResponseEntity.ok(ApiResponse.ok("Posiciones recuperadas con éxito", response));
    }


    @PatchMapping("/positions/{positionId}/status")
    @PreAuthorize("hasAuthority('LOCATIONS_UPDATE') or hasAuthority('INVENTORY_UPDATE') or hasAuthority('QUALITY_UPDATE') or hasAuthority('WAREHOUSE_MOVEMENTS_UPDATE') or hasRole('OPERATIONS_MANAGER') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_SUPERVISOR') or hasRole('WAREHOUSE_SUPERVISOR') or hasRole('SHIFT_LEADER') or hasRole('FORKLIFT_OPERATOR')")
    @Operation(summary = "Actualizar estado operativo de posición", description = "Ejecuta transiciones FSM: BLOCK (bloqueo QM), RELEASE (liberación) u OCCUPY.")
    public ResponseEntity<ApiResponse<PositionMapDetailResponse>> updatePositionStatus(
            @PathVariable UUID positionId,
            @Valid @RequestBody UpdatePositionStatusMapRequest request,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "SYSTEM";
        PositionMapDetailResponse response = mapUseCase.updatePositionStatus(positionId, request, username);
        return ResponseEntity.ok(ApiResponse.ok("Estado de la posición actualizado con éxito", response));
    }

    @PostMapping("/positions")
    @PreAuthorize("hasAuthority('LOCATIONS_CREATE') or hasAuthority('LAYOUT_UPDATE') or hasRole('OPERATIONS_MANAGER') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_SUPERVISOR') or hasRole('WAREHOUSE_SUPERVISOR')")
    @Operation(summary = "Dar de alta una nueva posición/bahía", description = "Crea una posición fija, temporal de buffer o precarga en la sección de almacén.")
    public ResponseEntity<ApiResponse<PositionMapDetailResponse>> createPosition(
            @Valid @RequestBody com.fourguard.wms.application.dto.request.map.CreatePositionMapRequest request,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "SYSTEM";
        PositionMapDetailResponse response = mapUseCase.createPosition(request, username);
        return ResponseEntity.ok(ApiResponse.ok("Posición registrada con éxito", response));
    }

    @PutMapping("/positions/{positionId}")
    @PreAuthorize("hasAuthority('LOCATIONS_UPDATE') or hasAuthority('LAYOUT_UPDATE') or hasRole('OPERATIONS_MANAGER') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_SUPERVISOR') or hasRole('WAREHOUSE_SUPERVISOR')")
    @Operation(summary = "Actualizar configuración y capacidad de una posición", description = "Modifica la capacidad en tarimas, categoría, o coordenadas físicas.")
    public ResponseEntity<ApiResponse<PositionMapDetailResponse>> updatePositionDetails(
            @PathVariable UUID positionId,
            @Valid @RequestBody com.fourguard.wms.application.dto.request.map.UpdatePositionDetailsMapRequest request,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "SYSTEM";
        PositionMapDetailResponse response = mapUseCase.updatePositionDetails(positionId, request, username);
        return ResponseEntity.ok(ApiResponse.ok("Posición actualizada con éxito", response));
    }

    @DeleteMapping("/positions/{positionId}")
    @PreAuthorize("hasAuthority('LOCATIONS_DELETE') or hasAuthority('LAYOUT_UPDATE') or hasRole('OPERATIONS_MANAGER') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_SUPERVISOR') or hasRole('WAREHOUSE_SUPERVISOR')")
    @Operation(summary = "Dar de baja una posición/bahía", description = "Inhabilita o da de baja lógica la posición si no tiene inventario activo.")
    public ResponseEntity<ApiResponse<Void>> deletePosition(
            @PathVariable UUID positionId,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "SYSTEM";
        mapUseCase.deletePosition(positionId, username);
        return ResponseEntity.ok(ApiResponse.ok("Posición dada de baja con éxito", null));
    }

    @GetMapping("/catalogs/block-reasons")
    @PreAuthorize("hasAuthority('INVENTORY_READ') or hasAuthority('LAYOUT_READ') or hasAuthority('QUALITY_READ') or hasAuthority('WAREHOUSE_MOVEMENTS_READ') or hasAuthority('RECEIVING_READ') or hasRole('OPERATIONS_MANAGER') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_SUPERVISOR') or hasRole('WAREHOUSE_SUPERVISOR') or hasRole('SHIFT_LEADER') or hasRole('WAREHOUSE_OPERATOR') or hasRole('SECURITY_GUARD') or hasRole('VIGILANCIA') or hasRole('FORKLIFT_OPERATOR') or hasRole('CONTROL_DESK') or hasRole('CEO')")
    @Operation(summary = "Obtener motivos de bloqueo QM", description = "Retorna el catálogo normalizado de causas de bloqueo para inspección de calidad.")
    public ResponseEntity<ApiResponse<List<CatBlockReasonResponse>>> getBlockReasons() {
        List<CatBlockReasonResponse> response = mapUseCase.getActiveBlockReasons();
        return ResponseEntity.ok(ApiResponse.ok("Catálogo de motivos de bloqueo recuperado con éxito", response));
    }
}
