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
    @PreAuthorize("hasAuthority('INVENTORY_READ') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Obtener topología 2D completa", description = "Retorna las naves del almacén con coordenadas SVG y métricas agregadas de ocupación.")
    public ResponseEntity<ApiResponse<WarehouseTopologyResponse>> getTopology(
            @RequestParam UUID branchId) {
        WarehouseTopologyResponse response = mapUseCase.getTopology(branchId);
        return ResponseEntity.ok(ApiResponse.ok("Topología del almacén recuperada con éxito", response));
    }

    @GetMapping("/sections/{sectionId}/positions")
    @PreAuthorize("hasAuthority('INVENTORY_READ') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Consultar posiciones de una sección", description = "Retorna la cuadrícula de posiciones de una nave con filtros de estado y búsqueda.")
    public ResponseEntity<ApiResponse<List<PositionMapDetailResponse>>> getPositionsBySection(
            @PathVariable UUID sectionId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {
        List<PositionMapDetailResponse> response = mapUseCase.getPositionsBySection(sectionId, status, search);
        return ResponseEntity.ok(ApiResponse.ok("Posiciones recuperadas con éxito", response));
    }

    @PatchMapping("/positions/{positionId}/status")
    @PreAuthorize("hasAuthority('LOCATIONS_UPDATE') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Actualizar estado operativo de posición", description = "Ejecuta transiciones FSM: BLOCK (bloqueo QM), RELEASE (liberación) u OCCUPY.")
    public ResponseEntity<ApiResponse<PositionMapDetailResponse>> updatePositionStatus(
            @PathVariable UUID positionId,
            @Valid @RequestBody UpdatePositionStatusMapRequest request,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "SYSTEM";
        PositionMapDetailResponse response = mapUseCase.updatePositionStatus(positionId, request, username);
        return ResponseEntity.ok(ApiResponse.ok("Estado de la posición actualizado con éxito", response));
    }

    @GetMapping("/catalogs/block-reasons")
    @PreAuthorize("hasAuthority('INVENTORY_READ') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Obtener motivos de bloqueo QM", description = "Retorna el catálogo normalizado de causas de bloqueo para inspección de calidad.")
    public ResponseEntity<ApiResponse<List<CatBlockReasonResponse>>> getBlockReasons() {
        List<CatBlockReasonResponse> response = mapUseCase.getActiveBlockReasons();
        return ResponseEntity.ok(ApiResponse.ok("Catálogo de motivos de bloqueo recuperado con éxito", response));
    }
}
