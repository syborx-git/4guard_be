package com.fourguard.wms.presentation.controller;

import com.fourguard.wms.application.dto.request.reception.*;
import com.fourguard.wms.application.dto.response.reception.*;
import com.fourguard.wms.domain.ports.in.WarehouseReceptionUseCase;
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

@RestController
@RequestMapping("/warehouse-receptions")
@RequiredArgsConstructor
@Tag(name = "Recepciones de Almacén", description = "Endpoints para la gestión del flujo transaccional de Recepción de Mercancía F01 (Inbound)")
public class WarehouseReceptionController {

    private final WarehouseReceptionUseCase receptionUseCase;

    // ─── CHECK-IN (CASETA) ──────────────────────────────────────────────────────

    @PostMapping("/check-in")
    @PreAuthorize("hasAuthority('WAREHOUSE_MOVEMENTS_CREATE') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER') or hasRole('SECURITY_GUARD')")
    @Operation(summary = "Alta de Pre-Recepción en Caseta",
               description = "Registra el arribo del transporte, chofer, placas, rampa y sellos. Genera automáticamente el folio en estado REGISTERED.")
    public ResponseEntity<ApiResponse<ReceptionResponse>> createCheckIn(
            @Valid @RequestBody CreateCheckInRequest request) {
        ReceptionResponse response = receptionUseCase.createCheckIn(request);
        return ResponseEntity.ok(ApiResponse.ok("Pre-recepción registrada con éxito", response));
    }

    // ─── UPDATE ANDÉN PARAMETERS ───────────────────────────────────────────────

    @PutMapping("/{id}/parameters")
    @PreAuthorize("hasAuthority('WAREHOUSE_MOVEMENTS_UPDATE') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER') or hasRole('WAREHOUSE_SUPERVISOR')")
    @Operation(summary = "Actualizar parámetros de descarga en andén",
               description = "Actualiza lote, caducidad, SKU, proveedor, piezas por tarima y ubicación sugerida.")
    public ResponseEntity<ApiResponse<ReceptionResponse>> updateParameters(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateReceptionParametersRequest request) {
        ReceptionResponse response = receptionUseCase.updateParameters(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Parámetros de recepción actualizados con éxito", response));
    }

    // ─── GET BY ID ─────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('WAREHOUSE_MOVEMENTS_READ') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER') or hasRole('WAREHOUSE_SUPERVISOR') or hasRole('FORKLIFT_OPERATOR')")
    @Operation(summary = "Obtener recepción por ID",
               description = "Retorna el detalle completo de la recepción, check-in, parámetros y lista de tarimas/UAs.")
    public ResponseEntity<ApiResponse<ReceptionResponse>> getReceptionById(@PathVariable UUID id) {
        ReceptionResponse response = receptionUseCase.getReceptionById(id);
        return ResponseEntity.ok(ApiResponse.ok("Recepción obtenida con éxito", response));
    }

    // ─── GET LIST WITH FILTERS ─────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAuthority('WAREHOUSE_MOVEMENTS_READ') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER') or hasRole('WAREHOUSE_SUPERVISOR')")
    @Operation(summary = "Listar recepciones",
               description = "Retorna el listado de recepciones filtrado por organización, sucursal, estatus y término de búsqueda.")
    public ResponseEntity<ApiResponse<List<ReceptionSummaryResponse>>> getReceptions(
            @RequestParam UUID organizationId,
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false, defaultValue = "ALL") String status,
            @RequestParam(required = false) String search) {
        List<ReceptionSummaryResponse> list = receptionUseCase.getReceptions(organizationId, branchId, status, search);
        return ResponseEntity.ok(ApiResponse.ok("Listado de recepciones obtenido con éxito", list));
    }

    // ─── ADD PALLETS (UAs) ─────────────────────────────────────────────────────

    @PostMapping("/{id}/pallets")
    @PreAuthorize("hasAuthority('WAREHOUSE_MOVEMENTS_UPDATE') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER') or hasRole('WAREHOUSE_SUPERVISOR')")
    @Operation(summary = "Agregar tarimas / UAs escaneadas",
               description = "Agrega uno o múltiples códigos de tarima escaneados a una recepción abierta.")
    public ResponseEntity<ApiResponse<List<ReceptionPalletResponse>>> addPallets(
            @PathVariable UUID id,
            @Valid @RequestBody AddReceptionPalletsRequest request) {
        List<ReceptionPalletResponse> pallets = receptionUseCase.addPallets(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Tarimas agregadas con éxito", pallets));
    }

    // ─── UPDATE PALLET ─────────────────────────────────────────────────────────

    @PutMapping("/{id}/pallets/{palletId}")
    @PreAuthorize("hasAuthority('WAREHOUSE_MOVEMENTS_UPDATE') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Editar tarima individual",
               description = "Modifica piezas, tipo de tarima u observaciones de una tarima capturada.")
    public ResponseEntity<ApiResponse<ReceptionPalletResponse>> updatePallet(
            @PathVariable UUID id,
            @PathVariable UUID palletId,
            @Valid @RequestBody UpdatePalletRequest request) {
        ReceptionPalletResponse response = receptionUseCase.updatePallet(id, palletId, request);
        return ResponseEntity.ok(ApiResponse.ok("Tarima actualizada con éxito", response));
    }

    // ─── DELETE PALLET ─────────────────────────────────────────────────────────

    @DeleteMapping("/{id}/pallets/{palletId}")
    @PreAuthorize("hasAuthority('WAREHOUSE_MOVEMENTS_UPDATE') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Eliminar tarima de la recepción",
               description = "Elimina una tarima de la recepción abierta.")
    public ResponseEntity<ApiResponse<Void>> deletePallet(
            @PathVariable UUID id,
            @PathVariable UUID palletId) {
        receptionUseCase.deletePallet(id, palletId);
        return ResponseEntity.ok(ApiResponse.ok("Tarima eliminada con éxito"));
    }

    // ─── COMPLETE RECEPTION (AUTORIZACIÓN LÍDER) ───────────────────────────────

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('WAREHOUSE_MOVEMENTS_AUTHORIZE') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER') or hasRole('WAREHOUSE_SUPERVISOR')")
    @Operation(summary = "Cierre y Autorización formal de Recepción F01",
               description = "Cierra la recepción con validación de credenciales del Líder/Supervisor de Almacén. Da de alta las UAs en el inventario activo.")
    public ResponseEntity<ApiResponse<ReceptionResponse>> completeReception(
            @PathVariable UUID id,
            @Valid @RequestBody CompleteReceptionRequest request) {
        ReceptionResponse response = receptionUseCase.completeReception(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Recepción autorizada y completada con éxito", response));
    }

    // ─── CANCEL RECEPTION (ADMIN) ──────────────────────────────────────────────

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('WAREHOUSE_MOVEMENTS_CANCEL') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Cancelación extraordinaria de recepción",
               description = "Revoca la recepción con validación de credenciales de Administrador y motivo obligatorio.")
    public ResponseEntity<ApiResponse<ReceptionResponse>> cancelReception(
            @PathVariable UUID id,
            @Valid @RequestBody CancelReceptionRequest request) {
        ReceptionResponse response = receptionUseCase.cancelReception(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Recepción cancelada con éxito", response));
    }

    // ─── CHANGE REMISIÓN ───────────────────────────────────────────────────────

    @PutMapping("/{id}/change-remision")
    @PreAuthorize("hasAuthority('WAREHOUSE_MOVEMENTS_UPDATE') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Modificar número de remisión / documento",
               description = "Actualiza el número de remisión de la recepción con justificación obligatoria y registro en auditoría.")
    public ResponseEntity<ApiResponse<ReceptionResponse>> changeRemision(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeRemisionRequest request) {
        ReceptionResponse response = receptionUseCase.changeRemision(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Número de remisión modificado con éxito", response));
    }

    // ─── AUDIT LOGS ───────────────────────────────────────────────────────────

    @GetMapping("/{id}/audit")
    @PreAuthorize("hasAuthority('WAREHOUSE_MOVEMENTS_READ') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER') or hasRole('WAREHOUSE_SUPERVISOR')")
    @Operation(summary = "Consultar trazabilidad y auditoría de la recepción",
               description = "Retorna la línea de tiempo de auditoría con deltas de cambios.")
    public ResponseEntity<ApiResponse<List<MovementAuditResponse>>> getAuditLogs(@PathVariable UUID id) {
        List<MovementAuditResponse> logs = receptionUseCase.getAuditLogs(id);
        return ResponseEntity.ok(ApiResponse.ok("Historial de auditoría obtenido con éxito", logs));
    }
}
