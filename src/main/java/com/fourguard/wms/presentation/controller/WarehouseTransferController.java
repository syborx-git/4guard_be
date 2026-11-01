package com.fourguard.wms.presentation.controller;

import com.fourguard.wms.application.dto.request.transfer.CancelTransferRequest;
import com.fourguard.wms.application.dto.request.transfer.CreateTransferRequest;
import com.fourguard.wms.application.dto.response.reception.MovementAuditResponse;
import com.fourguard.wms.application.dto.response.transfer.TransferResponse;
import com.fourguard.wms.application.dto.response.transfer.TransferSummaryResponse;
import com.fourguard.wms.domain.ports.in.WarehouseTransferUseCase;
import com.fourguard.wms.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/warehouse-transfers")
@RequiredArgsConstructor
@Tag(name = "Traspasos de Almacén", description = "Endpoints para la gestión de reubicaciones internas y cambio de almacén entre bahías")
public class WarehouseTransferController {

    private final WarehouseTransferUseCase transferUseCase;

    // ─── CREATE TRANSFER ────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAuthority('WAREHOUSE_MOVEMENTS_CREATE') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER') or hasRole('OPERATIONS_SUPERVISOR') or hasRole('WAREHOUSE_SUPERVISOR') or hasRole('SHIFT_LEADER') or hasRole('WAREHOUSE_OPERATOR') or hasRole('FORKLIFT_OPERATOR') or hasRole('CONTROL_DESK') or hasRole('CEO')")
    @Operation(summary = "Registrar cambio de almacén / traspaso", description = "Realiza la reubicación de tarimas entre bahías. Genera folio CAM-YYYY-XXXXXX y despacha evento SSE.")
    public ResponseEntity<ApiResponse<TransferResponse>> createTransfer(
            @Valid @RequestBody CreateTransferRequest request) {
        TransferResponse response = transferUseCase.createTransfer(request);
        return ResponseEntity.ok(ApiResponse.ok("Traspaso registrado con éxito", response));
    }

    // ─── GET BY ID ─────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('WAREHOUSE_MOVEMENTS_READ') or hasAuthority('INVENTORY_READ') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER') or hasRole('OPERATIONS_SUPERVISOR') or hasRole('WAREHOUSE_SUPERVISOR') or hasRole('SHIFT_LEADER') or hasRole('WAREHOUSE_OPERATOR') or hasRole('SECURITY_GUARD') or hasRole('VIGILANCIA') or hasRole('FORKLIFT_OPERATOR') or hasRole('CONTROL_DESK') or hasRole('CEO')")
    @Operation(summary = "Obtener traspaso por ID", description = "Retorna el detalle completo del traspaso y las tarimas reubicadas.")
    public ResponseEntity<ApiResponse<TransferResponse>> getTransferById(@PathVariable UUID id) {
        TransferResponse response = transferUseCase.getTransferById(id);
        return ResponseEntity.ok(ApiResponse.ok("Traspaso obtenido con éxito", response));
    }

    // ─── GET LIST ──────────────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAuthority('WAREHOUSE_MOVEMENTS_READ') or hasAuthority('INVENTORY_READ') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER') or hasRole('OPERATIONS_SUPERVISOR') or hasRole('WAREHOUSE_SUPERVISOR') or hasRole('SHIFT_LEADER') or hasRole('WAREHOUSE_OPERATOR') or hasRole('SECURITY_GUARD') or hasRole('VIGILANCIA') or hasRole('FORKLIFT_OPERATOR') or hasRole('CONTROL_DESK') or hasRole('CEO')")
    @Operation(summary = "Listar traspasos", description = "Retorna el historial de traspasos filtrado por organización, sucursal, estatus y término de búsqueda.")
    public ResponseEntity<ApiResponse<List<TransferSummaryResponse>>> getTransfers(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false, defaultValue = "ALL") String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID shiftSessionId,
            @RequestParam(required = false, defaultValue = "ALL") String scope) {
        List<TransferSummaryResponse> list = transferUseCase.getTransfers(
                organizationId, branchId, status, search, shiftSessionId,
                com.fourguard.wms.domain.enums.MovementScope.from(scope));
        return ResponseEntity.ok(ApiResponse.ok("Listado de traspasos obtenido con éxito", list));
    }

    // ─── LIFECYCLE TRANSITIONS (RF TERMINAL & CONTROL DESK) ─────────────────────

    @PostMapping("/{id}/start")
    @PreAuthorize("hasAuthority('WAREHOUSE_MOVEMENTS_UPDATE') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_SUPERVISOR') or hasRole('FORKLIFT_OPERATOR') or hasRole('CONTROL_DESK')")
    @Operation(summary = "Iniciar maniobra de traspaso en montacargas", description = "Pasa el estatus a IN_PROGRESS y estampa hora de inicio.")
    public ResponseEntity<ApiResponse<TransferResponse>> startTransfer(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, UUID> payload) {
        UUID operatorId = (payload != null) ? payload.get("operatorId") : null;
        TransferResponse response = transferUseCase.startTransfer(id, operatorId);
        return ResponseEntity.ok(ApiResponse.ok("Maniobra de traspaso iniciada", response));
    }

    @PostMapping("/{id}/pause")
    @PreAuthorize("hasAuthority('WAREHOUSE_MOVEMENTS_UPDATE') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_SUPERVISOR') or hasRole('FORKLIFT_OPERATOR') or hasRole('CONTROL_DESK')")
    @Operation(summary = "Pausar maniobra de traspaso", description = "Pausa la tarea por cambio de turno o interrupción urgente.")
    public ResponseEntity<ApiResponse<TransferResponse>> pauseTransfer(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> payload) {
        String reason = (payload != null) ? payload.get("reason") : "Pausa operativa / Fin de turno";
        String notes = (payload != null) ? payload.get("notes") : null;
        TransferResponse response = transferUseCase.pauseTransfer(id, reason, notes);
        return ResponseEntity.ok(ApiResponse.ok("Traspaso pausado correctamente", response));
    }

    @PostMapping("/{id}/resume")
    @PreAuthorize("hasAuthority('WAREHOUSE_MOVEMENTS_UPDATE') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_SUPERVISOR') or hasRole('FORKLIFT_OPERATOR') or hasRole('CONTROL_DESK')")
    @Operation(summary = "Reanudar maniobra de traspaso", description = "Reanuda un traspaso previamente pausado pasando a IN_PROGRESS.")
    public ResponseEntity<ApiResponse<TransferResponse>> resumeTransfer(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, UUID> payload) {
        UUID operatorId = (payload != null) ? payload.get("operatorId") : null;
        TransferResponse response = transferUseCase.resumeTransfer(id, operatorId);
        return ResponseEntity.ok(ApiResponse.ok("Traspaso reanudado", response));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('WAREHOUSE_MOVEMENTS_UPDATE') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_SUPERVISOR') or hasRole('FORKLIFT_OPERATOR') or hasRole('CONTROL_DESK')")
    @Operation(summary = "Completar depósito de traspaso", description = "Asienta la reubicación de inventario en Kardex y marca COMPLETED.")
    public ResponseEntity<ApiResponse<TransferResponse>> completeTransfer(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, UUID> payload) {
        UUID destId = (payload != null) ? payload.get("destinationLocationId") : null;
        TransferResponse response = transferUseCase.completeTransfer(id, destId);
        return ResponseEntity.ok(ApiResponse.ok("Traspaso completado con éxito", response));
    }

    @PatchMapping("/{id}/priority")
    @PreAuthorize("hasAuthority('WAREHOUSE_MOVEMENTS_UPDATE') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER') or hasRole('OPERATIONS_SUPERVISOR') or hasRole('CONTROL_DESK')")
    @Operation(summary = "Actualizar prioridad de traspaso", description = "Cambia prioridad a 'NORMAL' o 'URGENT' y despacha alarma a terminales RF.")
    public ResponseEntity<ApiResponse<TransferResponse>> updatePriority(
            @PathVariable UUID id,
            @RequestBody Map<String, String> payload) {
        String priority = payload.get("priority");
        TransferResponse response = transferUseCase.updatePriority(id, priority);
        return ResponseEntity.ok(ApiResponse.ok("Prioridad actualizada con éxito", response));
    }

    @PutMapping("/{id}/reassign")
    @PreAuthorize("hasAuthority('WAREHOUSE_MOVEMENTS_UPDATE') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_SUPERVISOR') or hasRole('CONTROL_DESK')")
    @Operation(summary = "Reasignar operador de montacargas", description = "Asigna dinámicamente un nuevo montacarguista a la maniobra.")
    public ResponseEntity<ApiResponse<TransferResponse>> reassignOperator(
            @PathVariable UUID id,
            @RequestBody Map<String, UUID> payload) {
        UUID operatorId = payload.get("forkliftOperatorId");
        TransferResponse response = transferUseCase.reassignOperator(id, operatorId);
        return ResponseEntity.ok(ApiResponse.ok("Operador reasignado con éxito", response));
    }

    // ─── CANCEL TRANSFER ───────────────────────────────────────────────────────

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('WAREHOUSE_MOVEMENTS_CANCEL') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Cancelar traspaso", description = "Revoca el traspaso reubicando las tarimas a su bahía de origen.")
    public ResponseEntity<ApiResponse<TransferResponse>> cancelTransfer(
            @PathVariable UUID id,
            @Valid @RequestBody CancelTransferRequest request) {
        TransferResponse response = transferUseCase.cancelTransfer(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Traspaso cancelado con éxito", response));
    }

    // ─── AUDIT LOGS ───────────────────────────────────────────────────────────

    @GetMapping("/{id}/audit")
    @PreAuthorize("hasAuthority('WAREHOUSE_MOVEMENTS_READ') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER') or hasRole('OPERATIONS_SUPERVISOR') or hasRole('WAREHOUSE_SUPERVISOR')")
    @Operation(summary = "Consultar auditoría del traspaso", description = "Retorna la línea de tiempo de auditoría del traspaso.")
    public ResponseEntity<ApiResponse<List<MovementAuditResponse>>> getAuditLogs(@PathVariable UUID id) {
        List<MovementAuditResponse> logs = transferUseCase.getAuditLogs(id);
        return ResponseEntity.ok(ApiResponse.ok("Historial de auditoría obtenido con éxito", logs));
    }
}
