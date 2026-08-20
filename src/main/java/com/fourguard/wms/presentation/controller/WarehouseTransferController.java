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
import java.util.UUID;

@RestController
@RequestMapping("/warehouse-transfers")
@RequiredArgsConstructor
@Tag(name = "Traspasos de Almacén", description = "Endpoints para la gestión de reubicaciones internas y cambio de almacén entre bahías")
public class WarehouseTransferController {

    private final WarehouseTransferUseCase transferUseCase;

    // ─── CREATE TRANSFER ────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAuthority('WAREHOUSE_MOVEMENTS_CREATE') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER') or hasRole('WAREHOUSE_SUPERVISOR')")
    @Operation(summary = "Registrar cambio de almacén / traspaso",
               description = "Realiza la reubicación de tarimas entre bahías. Genera folio CAM-YYYY-XXXXXX y actualiza el inventario.")
    public ResponseEntity<ApiResponse<TransferResponse>> createTransfer(
            @Valid @RequestBody CreateTransferRequest request) {
        TransferResponse response = transferUseCase.createTransfer(request);
        return ResponseEntity.ok(ApiResponse.ok("Traspaso registrado con éxito", response));
    }

    // ─── GET BY ID ─────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('WAREHOUSE_MOVEMENTS_READ') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER') or hasRole('WAREHOUSE_SUPERVISOR') or hasRole('FORKLIFT_OPERATOR')")
    @Operation(summary = "Obtener traspaso por ID",
               description = "Retorna el detalle completo del traspaso y las tarimas reubicadas.")
    public ResponseEntity<ApiResponse<TransferResponse>> getTransferById(@PathVariable UUID id) {
        TransferResponse response = transferUseCase.getTransferById(id);
        return ResponseEntity.ok(ApiResponse.ok("Traspaso obtenido con éxito", response));
    }

    // ─── GET LIST ──────────────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAuthority('WAREHOUSE_MOVEMENTS_READ') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER') or hasRole('WAREHOUSE_SUPERVISOR')")
    @Operation(summary = "Listar traspasos",
               description = "Retorna el historial de traspasos filtrado por organización, sucursal, estatus y término de búsqueda.")
    public ResponseEntity<ApiResponse<List<TransferSummaryResponse>>> getTransfers(
            @RequestParam UUID organizationId,
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false, defaultValue = "ALL") String status,
            @RequestParam(required = false) String search) {
        List<TransferSummaryResponse> list = transferUseCase.getTransfers(organizationId, branchId, status, search);
        return ResponseEntity.ok(ApiResponse.ok("Listado de traspasos obtenido con éxito", list));
    }

    // ─── CANCEL TRANSFER ───────────────────────────────────────────────────────

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('WAREHOUSE_MOVEMENTS_CANCEL') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Cancelar traspaso",
               description = "Revoca el traspaso reubicando las tarimas a su bahía de origen.")
    public ResponseEntity<ApiResponse<TransferResponse>> cancelTransfer(
            @PathVariable UUID id,
            @Valid @RequestBody CancelTransferRequest request) {
        TransferResponse response = transferUseCase.cancelTransfer(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Traspaso cancelado con éxito", response));
    }

    // ─── AUDIT LOGS ───────────────────────────────────────────────────────────

    @GetMapping("/{id}/audit")
    @PreAuthorize("hasAuthority('WAREHOUSE_MOVEMENTS_READ') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER') or hasRole('WAREHOUSE_SUPERVISOR')")
    @Operation(summary = "Consultar auditoría del traspaso",
               description = "Retorna la línea de tiempo de auditoría del traspaso.")
    public ResponseEntity<ApiResponse<List<MovementAuditResponse>>> getAuditLogs(@PathVariable UUID id) {
        List<MovementAuditResponse> logs = transferUseCase.getAuditLogs(id);
        return ResponseEntity.ok(ApiResponse.ok("Historial de auditoría obtenido con éxito", logs));
    }
}
