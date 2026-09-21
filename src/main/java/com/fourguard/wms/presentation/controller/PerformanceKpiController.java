package com.fourguard.wms.presentation.controller;

import com.fourguard.wms.application.dto.response.performance.PerformanceMetricsDto.*;
import com.fourguard.wms.application.usecase.PerformanceKpiManagementService;
import com.fourguard.wms.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/performance-kpis", "/performance/kpis"})
@RequiredArgsConstructor
@Tag(name = "Catálogo y Gestión de KPIs (HU-138)", description = "CRUD de reglas, umbrales y semáforos de KPIs en base de datos")
public class PerformanceKpiController {

    private final PerformanceKpiManagementService kpiManagementService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Listar catálogo de KPIs desde BD", description = "Retorna lista de KPIs filtrados por módulo o búsqueda")
    public ResponseEntity<ApiResponse<List<KpiResponseDto>>> listKpis(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String search) {
        List<KpiResponseDto> list = kpiManagementService.listKpis(organizationId, module, search);
        return ResponseEntity.ok(ApiResponse.ok("Catálogo de KPIs obtenido con éxito", list));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'SUPERVISOR')")
    @Operation(summary = "Crear nuevo KPI en BD", description = "Guarda una nueva regla y umbrales en base de datos")
    public ResponseEntity<ApiResponse<KpiResponseDto>> createKpi(
            @RequestParam(required = false) UUID organizationId,
            @RequestBody CreateKpiRequestDto dto,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "admin";
        KpiResponseDto created = kpiManagementService.createKpi(organizationId, dto, username);
        return ResponseEntity.ok(ApiResponse.ok("KPI registrado con éxito en base de datos", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'SUPERVISOR')")
    @Operation(summary = "Actualizar KPI en BD", description = "Actualiza umbrales y reglas de negocio de un KPI")
    public ResponseEntity<ApiResponse<KpiResponseDto>> updateKpi(
            @RequestParam(required = false) UUID organizationId,
            @PathVariable UUID id,
            @RequestBody UpdateKpiRequestDto dto,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "admin";
        KpiResponseDto updated = kpiManagementService.updateKpi(organizationId, id, dto, username);
        return ResponseEntity.ok(ApiResponse.ok("KPI actualizado con éxito en base de datos", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Desactivar KPI (Borrado lógico)", description = "Desactiva el indicador en base de datos")
    public ResponseEntity<ApiResponse<Void>> disableKpi(
            @RequestParam(required = false) UUID organizationId,
            @PathVariable UUID id,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "admin";
        kpiManagementService.disableKpi(organizationId, id, username);
        return ResponseEntity.ok(ApiResponse.ok("KPI desactivado con éxito", null));
    }
}
