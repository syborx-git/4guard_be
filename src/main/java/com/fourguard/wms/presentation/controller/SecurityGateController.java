package com.fourguard.wms.presentation.controller;

import com.fourguard.wms.application.dto.request.security.DriverCheckinSubmissionRequest;
import com.fourguard.wms.application.dto.request.security.GeneratePassRequest;
import com.fourguard.wms.application.dto.request.security.GuardCheckinCompletionRequest;
import com.fourguard.wms.application.dto.request.security.GuardCheckOutRequest;
import com.fourguard.wms.application.dto.response.security.PassResponse;
import com.fourguard.wms.application.dto.response.security.SecurityGatePublicCatalogsResponse;
import com.fourguard.wms.domain.ports.in.SecurityGateUseCase;
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
@RequestMapping("/security-gate")
@RequiredArgsConstructor
@Tag(name = "Caseta de Seguridad (Gate Check-In & QR Passes)",
     description = "Endpoints para la gestión de pases de acceso QR y auto-registro de transportistas (F01-PO-CP-7.1.3-03)")
public class SecurityGateController {

    private final SecurityGateUseCase securityGateUseCase;

    // ─── PUBLIC DRIVER PORTAL ENDPOINTS ───────────────────────────────────────

    @GetMapping("/public/catalogs")
    @Operation(summary = "Obtener catálogos dinámicos públicos para transportistas (Clientes, Líneas, Tipos de Transporte)",
               description = "Retorna la lista de clientes activos, líneas transportistas y tipos de transporte permitidos.")
    public ResponseEntity<ApiResponse<SecurityGatePublicCatalogsResponse>> getPublicCatalogs(
            @RequestParam(required = false) UUID organizationId) {
        SecurityGatePublicCatalogsResponse response = securityGateUseCase.getPublicCatalogs(organizationId);
        return ResponseEntity.ok(ApiResponse.ok("Catálogos de caseta obtenidos con éxito", response));
    }

    @GetMapping("/public/passes/{token}")
    @Operation(summary = "Consultar pase QR por Token (Acceso Público Móvil Chofer)",
               description = "Permite al dispositivo móvil del chofer obtener los datos del pase y el formulario F01-PO-CP-7.1.3-03.")
    public ResponseEntity<ApiResponse<PassResponse>> getPassByToken(@PathVariable String token) {
        PassResponse response = securityGateUseCase.getPassByToken(token);
        return ResponseEntity.ok(ApiResponse.ok("Pase de acceso obtenido con éxito", response));
    }

    @PostMapping("/public/passes/{token}/submit")
    @Operation(summary = "Enviar auto-registro de transportista (Acceso Público Móvil Chofer)",
               description = "Guarda los datos del vehículo, checklist de inspección y firma digital del transportista.")
    public ResponseEntity<ApiResponse<PassResponse>> submitDriverCheckin(
            @PathVariable String token,
            @Valid @RequestBody DriverCheckinSubmissionRequest request) {
        PassResponse response = securityGateUseCase.submitDriverCheckin(token, request);
        return ResponseEntity.ok(ApiResponse.ok("Auto-registro enviado con éxito a Caseta de Seguridad", response));
    }

    // ─── GUARD / ADMIN AUTHENTICATED ENDPOINTS ────────────────────────────────

    @PostMapping("/passes/generate")
    @PreAuthorize("hasAuthority('SECURITY_GATE_CREATE') or hasRole('SECURITY_GUARD') or hasRole('VIGILANCIA') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER') or hasRole('OPERATIONS_SUPERVISOR')")
    @Operation(summary = "Generar nuevo Pase Digital QR para Chofer",
               description = "Crea un token único de acceso con código QR para que el chofer complete el formato de acceso en su smartphone.")
    public ResponseEntity<ApiResponse<PassResponse>> generatePass(
            @Valid @RequestBody GeneratePassRequest request) {
        PassResponse response = securityGateUseCase.generatePass(request);
        return ResponseEntity.ok(ApiResponse.ok("Pase digital QR generado con éxito", response));
    }

    @GetMapping("/passes/active")
    @PreAuthorize("hasAuthority('SECURITY_GATE_READ') or hasRole('SECURITY_GUARD') or hasRole('VIGILANCIA') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER') or hasRole('OPERATIONS_SUPERVISOR')")
    @Operation(summary = "Listar pases activos y choferes en espera",
               description = "Retorna la cola de choferes en espera o con auto-registro enviado para revisión del guardia.")
    public ResponseEntity<ApiResponse<List<PassResponse>>> getActivePasses(
            @RequestParam UUID organizationId,
            @RequestParam(required = false) UUID branchId) {
        List<PassResponse> list = securityGateUseCase.getActivePasses(organizationId, branchId);
        return ResponseEntity.ok(ApiResponse.ok("Pases activos obtenidos con éxito", list));
    }

    @GetMapping("/passes/in-yard")
    @PreAuthorize("hasAuthority('SECURITY_GATE_READ') or hasRole('SECURITY_GUARD') or hasRole('VIGILANCIA') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER') or hasRole('OPERATIONS_SUPERVISOR')")
    @Operation(summary = "Listar unidades en planta (patio / andenes)",
               description = "Retorna los vehículos que han ingresado y están actualmente en descarga, carga o listos para salida.")
    public ResponseEntity<ApiResponse<List<PassResponse>>> getInYardPasses(
            @RequestParam UUID organizationId,
            @RequestParam(required = false) UUID branchId) {
        List<PassResponse> list = securityGateUseCase.getInYardPasses(organizationId, branchId);
        return ResponseEntity.ok(ApiResponse.ok("Unidades en planta obtenidas con éxito", list));
    }

    @GetMapping("/passes/history")
    @PreAuthorize("hasAuthority('SECURITY_GATE_READ') or hasRole('SECURITY_GUARD') or hasRole('VIGILANCIA') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER') or hasRole('OPERATIONS_SUPERVISOR')")
    @Operation(summary = "Historial y Auditoría de Caseta de Seguridad",
               description = "Retorna el histórico completo de pases y movimientos con búsqueda para reimpresión de boleta F01.")
    public ResponseEntity<ApiResponse<List<PassResponse>>> getHistoryPasses(
            @RequestParam UUID organizationId,
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false) String search) {
        List<PassResponse> list = securityGateUseCase.getHistoryPasses(organizationId, branchId, search);
        return ResponseEntity.ok(ApiResponse.ok("Historial de caseta obtenido con éxito", list));
    }

    @PostMapping("/passes/{token}/complete")
    @PreAuthorize("hasAuthority('SECURITY_GATE_UPDATE') or hasRole('SECURITY_GUARD') or hasRole('VIGILANCIA') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER') or hasRole('OPERATIONS_SUPERVISOR')")
    @Operation(summary = "Validar y autorizar entrada a planta (Check-In Oficial WMS)",
               description = "El guardia confirma los datos, asigna el andén/rampa y genera el folio oficial de Recepción o Salida en el WMS.")
    public ResponseEntity<ApiResponse<PassResponse>> completeCheckin(
            @PathVariable String token,
            @Valid @RequestBody GuardCheckinCompletionRequest request) {
        PassResponse response = securityGateUseCase.completeCheckin(token, request);
        return ResponseEntity.ok(ApiResponse.ok("Check-in autorizado y registrado exitosamente en el WMS", response));
    }

    @PostMapping("/passes/{token}/check-out")
    @PreAuthorize("hasAuthority('SECURITY_GATE_UPDATE') or hasRole('SECURITY_GUARD') or hasRole('VIGILANCIA') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER') or hasRole('OPERATIONS_SUPERVISOR')")
    @Operation(summary = "Registrar Salida de Planta (Check-Out Oficial y Cierre de Pase F01)",
               description = "El guardia registra la hora de salida de la unidad, observaciones finales y sella la salida para imprimir o descargar el formato F01.")
    public ResponseEntity<ApiResponse<PassResponse>> checkOut(
            @PathVariable String token,
            @RequestBody(required = false) GuardCheckOutRequest request) {
        GuardCheckOutRequest req = request != null ? request : new GuardCheckOutRequest();
        PassResponse response = securityGateUseCase.checkOut(token, req);
        return ResponseEntity.ok(ApiResponse.ok("Salida registrada exitosamente. Unidad lista para retiro y generación de boleta F01.", response));
    }

    @DeleteMapping("/passes/{id}")
    @PreAuthorize("hasAuthority('SECURITY_GATE_DELETE') or hasAuthority('SECURITY_GATE_UPDATE') or hasAuthority('SECURITY_GATE_CREATE') or hasRole('SECURITY_GUARD') or hasRole('VIGILANCIA') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER') or hasRole('OPERATIONS_SUPERVISOR')")
    @Operation(summary = "Cancelar / Descartar Pase Digital QR",
               description = "Elimina un pase de acceso generado o pendiente que no fue utilizado.")
    public ResponseEntity<ApiResponse<Void>> deletePass(@PathVariable UUID id) {
        securityGateUseCase.cancelPass(id);
        return ResponseEntity.ok(ApiResponse.ok("Pase descartado exitosamente"));
    }
}
