package com.fourguard.wms.presentation.controller;

import com.fourguard.wms.application.dto.request.outbound.CancelOutboundRequest;
import com.fourguard.wms.application.dto.request.outbound.CreateOutboundRequest;
import com.fourguard.wms.application.dto.request.outbound.ValidatePalletsRequest;
import com.fourguard.wms.application.dto.request.reception.ChangeRemisionRequest;
import com.fourguard.wms.application.dto.response.outbound.InventoryBatchResponse;
import com.fourguard.wms.application.dto.response.outbound.OutboundResponse;
import com.fourguard.wms.application.dto.response.outbound.OutboundSummaryResponse;
import com.fourguard.wms.application.dto.response.outbound.ScanPalletResponse;
import com.fourguard.wms.application.dto.response.reception.MovementAuditResponse;
import com.fourguard.wms.domain.ports.in.WarehouseOutboundUseCase;
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
@RequestMapping("/warehouse-outbounds")
@RequiredArgsConstructor
@Tag(name = "Salidas de Almacén", description = "Endpoints para la gestión de despachos y salidas de inventario F03 (Outbound)")
public class WarehouseOutboundController {

    private final WarehouseOutboundUseCase outboundUseCase;

    // ─── CREATE OUTBOUND ────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAuthority('WAREHOUSE_MOVEMENTS_CREATE') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER') or hasRole('WAREHOUSE_SUPERVISOR')")
    @Operation(summary = "Registrar salida de almacén / despacho",
               description = "Registra la salida física y lógica de mercancía. Descuenta las UAs del inventario y genera folio SAL-YYYY-XXXXXX.")
    public ResponseEntity<ApiResponse<OutboundResponse>> createOutbound(
            @Valid @RequestBody CreateOutboundRequest request) {
        OutboundResponse response = outboundUseCase.createOutbound(request);
        return ResponseEntity.ok(ApiResponse.ok("Salida registrada con éxito", response));
    }

    // ─── GET BY ID ─────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('WAREHOUSE_MOVEMENTS_READ') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER') or hasRole('WAREHOUSE_SUPERVISOR') or hasRole('FORKLIFT_OPERATOR')")
    @Operation(summary = "Obtener salida por ID",
               description = "Retorna el detalle completo de la salida y las tarimas despachadas.")
    public ResponseEntity<ApiResponse<OutboundResponse>> getOutboundById(@PathVariable UUID id) {
        OutboundResponse response = outboundUseCase.getOutboundById(id);
        return ResponseEntity.ok(ApiResponse.ok("Salida obtenida con éxito", response));
    }

    // ─── UPDATE OUTBOUND ───────────────────────────────────────────────────────

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('WAREHOUSE_MOVEMENTS_UPDATE') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER') or hasRole('WAREHOUSE_SUPERVISOR') or hasRole('FORKLIFT_OPERATOR')")
    @Operation(summary = "Actualizar salida de almacén / transicionar fase",
               description = "Actualiza el estatus del ciclo operativo (REGISTERED -> ASSIGNED -> IN_PROGRESS -> LOADED -> COMPLETED), rampa, montacarguista o datos de caseta.")
    public ResponseEntity<ApiResponse<OutboundResponse>> updateOutbound(
            @PathVariable UUID id,
            @RequestBody com.fourguard.wms.application.dto.request.outbound.UpdateOutboundRequest request) {
        OutboundResponse response = outboundUseCase.updateOutbound(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Salida actualizada con éxito", response));
    }

    // ─── GET LIST ──────────────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAuthority('WAREHOUSE_MOVEMENTS_READ') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER') or hasRole('WAREHOUSE_SUPERVISOR')")
    @Operation(summary = "Listar salidas de almacén",
               description = "Retorna el historial de salidas filtrado por organización, sucursal, estatus y búsqueda.")
    public ResponseEntity<ApiResponse<List<OutboundSummaryResponse>>> getOutbounds(
            @RequestParam UUID organizationId,
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false, defaultValue = "ALL") String status,
            @RequestParam(required = false) String search) {
        List<OutboundSummaryResponse> list = outboundUseCase.getOutbounds(organizationId, branchId, status, search);
        return ResponseEntity.ok(ApiResponse.ok("Listado de salidas obtenido con éxito", list));
    }

    // ─── CANCEL OUTBOUND ───────────────────────────────────────────────────────

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('WAREHOUSE_MOVEMENTS_CANCEL') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Cancelar salida de almacén",
               description = "Revoca el despacho y restaura las tarimas al inventario disponible.")
    public ResponseEntity<ApiResponse<OutboundResponse>> cancelOutbound(
            @PathVariable UUID id,
            @Valid @RequestBody CancelOutboundRequest request) {
        OutboundResponse response = outboundUseCase.cancelOutbound(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Salida cancelada con éxito", response));
    }

    // ─── CHANGE REMISIÓN / CARTA PORTE ─────────────────────────────────────────

    @PutMapping({"/{id}/change-shipping-note", "/{id}/change-document", "/{id}/change-waybill", "/{id}/change-remision"})
    @PreAuthorize("hasAuthority('WAREHOUSE_MOVEMENTS_UPDATE') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Modificar número de remisión / carta porte de salida",
               description = "Actualiza el número de remisión / carta porte de la salida con justificación y registro en auditoría.")
    public ResponseEntity<ApiResponse<OutboundResponse>> changeRemision(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeRemisionRequest request) {
        OutboundResponse response = outboundUseCase.changeRemision(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Número de remisión / carta porte modificado con éxito", response));
    }

    // ─── INVENTORY BATCHES (FIFO / FEFO) ───────────────────────────────────────

    @GetMapping("/inventory-batches")
    @PreAuthorize("hasAuthority('WAREHOUSE_MOVEMENTS_READ') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER') or hasRole('WAREHOUSE_SUPERVISOR')")
    @Operation(summary = "Consultar lotes disponibles con sugerencia FIFO/FEFO",
               description = "Retorna los lotes de inventario agrupados con ordenamiento FEFO indexado y búsqueda rápida.")
    public ResponseEntity<ApiResponse<List<InventoryBatchResponse>>> getInventoryBatches(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false) UUID clientId,
            @RequestParam(required = false) UUID skuId,
            @RequestParam(required = false) String search) {
        List<InventoryBatchResponse> batches = outboundUseCase.getInventoryBatches(organizationId, branchId, clientId, skuId, search);
        return ResponseEntity.ok(ApiResponse.ok("Lotes de inventario obtenidos con éxito", batches));
    }

    // ─── SCAN PALLET (FAST RF BARCODE LOOKUP) ──────────────────────────────────

    @GetMapping("/scan-pallet")
    @PreAuthorize("hasAuthority('WAREHOUSE_MOVEMENTS_READ') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER') or hasRole('WAREHOUSE_SUPERVISOR') or hasRole('FORKLIFT_OPERATOR')")
    @Operation(summary = "Escanear / Consultar tarima o UA por código de barras",
               description = "Búsqueda rápida en sub-10ms por SSCC o código de barras de tarima para escaneo masivo con pistola RF o tablet.")
    public ResponseEntity<ApiResponse<ScanPalletResponse>> scanPallet(
            @RequestParam String barcode,
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) UUID branchId) {
        ScanPalletResponse response = outboundUseCase.scanPallet(barcode, organizationId, branchId);
        return ResponseEntity.ok(ApiResponse.ok("Tarima encontrada", response));
    }

    // ─── VALIDATE PALLETS (BATCH LOOKUP) ───────────────────────────────────────

    @PostMapping("/validate-pallets")
    @PreAuthorize("hasAuthority('WAREHOUSE_MOVEMENTS_READ') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER') or hasRole('WAREHOUSE_SUPERVISOR') or hasRole('FORKLIFT_OPERATOR')")
    @Operation(summary = "Validar lote de tarimas escaneadas",
               description = "Valida y retorna metadatos de un listado de códigos de barras / SSCCs escaneados.")
    public ResponseEntity<ApiResponse<List<ScanPalletResponse>>> validatePallets(
            @Valid @RequestBody ValidatePalletsRequest request) {
        List<ScanPalletResponse> response = outboundUseCase.validatePallets(request);
        return ResponseEntity.ok(ApiResponse.ok("Tarimas validadas con éxito", response));
    }

    // ─── AUDIT LOGS ───────────────────────────────────────────────────────────

    @GetMapping("/{id}/audit")
    @PreAuthorize("hasAuthority('WAREHOUSE_MOVEMENTS_READ') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER') or hasRole('WAREHOUSE_SUPERVISOR')")
    @Operation(summary = "Consultar auditoría de la salida",
               description = "Retorna la línea de tiempo de auditoría del despacho.")
    public ResponseEntity<ApiResponse<List<MovementAuditResponse>>> getAuditLogs(@PathVariable UUID id) {
        List<MovementAuditResponse> logs = outboundUseCase.getAuditLogs(id);
        return ResponseEntity.ok(ApiResponse.ok("Historial de auditoría obtenido con éxito", logs));
    }
}
