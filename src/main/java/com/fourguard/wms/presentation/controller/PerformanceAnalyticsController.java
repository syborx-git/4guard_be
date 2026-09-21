package com.fourguard.wms.presentation.controller;

import com.fourguard.wms.application.dto.response.performance.PerformanceMetricsDto.*;
import com.fourguard.wms.application.usecase.PerformanceAnalyticsService;
import com.fourguard.wms.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/performance")
@RequiredArgsConstructor
@Tag(name = "Módulo de Rendimiento y Analítica WMS", description = "Endpoints analíticos para monitoreo en tiempo real, productividad de operadores y KPIs 3PL (HU-9, HU-138, HU-141, HU-159)")
public class PerformanceAnalyticsController {

    private final PerformanceAnalyticsService performanceAnalyticsService;

    @GetMapping("/executive-kpi")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener 5 KPIs Ejecutivos en tiempo real (Polling 60s)",
               description = "Retorna métricas de Ocupación, IRA %, OTIF %, tiempos de ciclo y volumen de operaciones.")
    public ResponseEntity<ApiResponse<ExecutiveKpiResponse>> getExecutiveKpi(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate) {
        ExecutiveKpiResponse response = performanceAnalyticsService.getExecutiveKpi(organizationId, branchId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.ok("Métricas ejecutivas obtenidas con éxito", response));
    }

    @GetMapping("/inbound-times")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener tiempos de descarga y métricas Inbound",
               description = "Retorna tiempos promedio de descarga por rampa, mínimos y máximos.")
    public ResponseEntity<ApiResponse<InboundProcessTimesResponse>> getInboundTimes(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate) {
        InboundProcessTimesResponse response = performanceAnalyticsService.getInboundProcessTimes(organizationId, branchId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.ok("Tiempos de recepción obtenidos con éxito", response));
    }

    @GetMapping("/operator-ranking")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener ranking de productividad de montacarguistas",
               description = "Retorna lista de operadores con tarimas/hora (PPH), turno y nivel de desempeño.")
    public ResponseEntity<ApiResponse<List<OperatorProductivityResponse>>> getOperatorRanking(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false) UUID shiftId) {
        List<OperatorProductivityResponse> response = performanceAnalyticsService.getOperatorRanking(organizationId, branchId, shiftId);
        return ResponseEntity.ok(ApiResponse.ok("Ranking de operadores obtenido con éxito", response));
    }

    @GetMapping("/shift-productivity")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener resumen de productividad por turno",
               description = "Retorna métricas consolidadas de operadores activos, movimientos y porcentaje de meta por turno.")
    public ResponseEntity<ApiResponse<List<ShiftProductivitySummaryResponse>>> getShiftProductivity(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) UUID branchId) {
        List<ShiftProductivitySummaryResponse> response = performanceAnalyticsService.getShiftProductivitySummaries(organizationId, branchId);
        return ResponseEntity.ok(ApiResponse.ok("Productividad por turno obtenida con éxito", response));
    }

    @GetMapping("/process-flow-times")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener tiempos de los 4 procesos nodales",
               description = "Retorna duración promedio y porcentaje de cumplimiento para Recepción, Putaway, Picking y Embarque.")
    public ResponseEntity<ApiResponse<List<ProcessFlowTimesResponse>>> getProcessFlowTimes(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) UUID branchId) {
        List<ProcessFlowTimesResponse> response = performanceAnalyticsService.getProcessFlowTimes(organizationId, branchId);
        return ResponseEntity.ok(ApiResponse.ok("Tiempos de procesos obtenidos con éxito", response));
    }

    @GetMapping({"/delicate-circuit", "/circuito-delicado", "/dedicated-fleet"})
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener métricas del Circuito Delicado (10 Choferes & 7 Unidades)",
               description = "Retorna consolidado de viajes, rotación de cajas, volumen y tiempos promedio de vuelta para transporte propio.")
    public ResponseEntity<ApiResponse<CircuitoDelicadoSummaryResponse>> getCircuitoDelicado(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) UUID branchId) {
        CircuitoDelicadoSummaryResponse response = performanceAnalyticsService.getCircuitoDelicado(organizationId, branchId);
        return ResponseEntity.ok(ApiResponse.ok("Métricas del Circuito Delicado obtenidas con éxito", response));
    }

    @GetMapping("/gate-to-gate-cycle")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener métricas de ciclo Puerta a Puerta y Candados de Calidad (HU-151 -> HU-154)",
               description = "Retorna tiempos discriminados desde escaneo QR en vigilancia hasta salida de caseta y validación de peso/F01.")
    public ResponseEntity<ApiResponse<GateToGateCycleResponse>> getGateToGateCycle(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) UUID branchId) {
        GateToGateCycleResponse response = performanceAnalyticsService.getGateToGateCycle(organizationId, branchId);
        return ResponseEntity.ok(ApiResponse.ok("Ciclo Puerta a Puerta obtenido con éxito", response));
    }

    @GetMapping("/timeline-audit")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener línea de tiempo auditada cronológica por folio/SSCC",
               description = "Retorna historial segundo a segundo de acciones de almacén con operadores, tarimas y estatus de candados.")
    public ResponseEntity<ApiResponse<List<MovementAuditTimelineDto>>> getMovementAuditTimeline(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false) String folio,
            @RequestParam(required = false) String sscc,
            @RequestParam(required = false) UUID operatorId) {
        List<MovementAuditTimelineDto> response = performanceAnalyticsService.getMovementAuditTimeline(organizationId, branchId, folio, sscc, operatorId);
        return ResponseEntity.ok(ApiResponse.ok("Línea de tiempo auditada obtenida con éxito", response));
    }

    @PostMapping("/export-jobs")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Encolar job asíncrono para exportación Excel de 21 columnas (HU-158)",
               description = "Encola la generación de reporte de rendimiento con firma criptográfica y devuelve identificador de descarga.")
    public ResponseEntity<ApiResponse<ExportJobResponseDto>> enqueueExportJob(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) UUID branchId,
            @RequestParam(defaultValue = "FULL_PERFORMANCE_21COL") String reportType) {
        ExportJobResponseDto response = performanceAnalyticsService.enqueueExportJob(organizationId, branchId, reportType);
        return ResponseEntity.ok(ApiResponse.ok("Job de exportación encolado exitosamente", response));
    }

    @GetMapping("/targets")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener calibración de metas operativas personalizadas (HU-138)",
               description = "Retorna los umbrales de meta activos para Ocupación, IRA, OTIF, Dock-to-Stock y PPH.")
    public ResponseEntity<ApiResponse<OperationalUserTargetsDto>> getTargets(
            @RequestParam(required = false) UUID organizationId) {
        OperationalUserTargetsDto targets = performanceAnalyticsService.getUserTargets(organizationId);
        return ResponseEntity.ok(ApiResponse.ok("Metas operativas obtenidas con éxito", targets));
    }

    @PutMapping("/targets")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'SUPERVISOR', 'OPERATIONS_MANAGER')")
    @Operation(summary = "Guardar y actualizar metas operativas personalizadas (HU-138)",
               description = "Persiste los nuevos umbrales calibrados por el usuario y los sincroniza en base de datos.")
    public ResponseEntity<ApiResponse<OperationalUserTargetsDto>> updateTargets(
            @RequestParam(required = false) UUID organizationId,
            @RequestBody OperationalUserTargetsDto dto,
            org.springframework.security.core.Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "admin";
        OperationalUserTargetsDto saved = performanceAnalyticsService.saveUserTargets(organizationId, dto, username);
        return ResponseEntity.ok(ApiResponse.ok("Metas operativas actualizadas y guardadas con éxito en base de datos", saved));
    }
}
