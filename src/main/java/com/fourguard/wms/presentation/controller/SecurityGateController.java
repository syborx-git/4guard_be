package com.fourguard.wms.presentation.controller;

import com.fourguard.wms.application.dto.request.security.DriverCheckinSubmissionRequest;
import com.fourguard.wms.application.dto.request.security.GeneratePassRequest;
import com.fourguard.wms.application.dto.request.security.GuardCheckinCompletionRequest;
import com.fourguard.wms.application.dto.response.security.PassResponse;
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
@RequestMapping("/api/v1/security-gate")
@RequiredArgsConstructor
@Tag(name = "Caseta de Seguridad (Gate Check-In & QR Passes)",
     description = "Endpoints para la gestión de pases de acceso QR y auto-registro de transportistas (F01-PO-CP-7.1.3-03)")
public class SecurityGateController {

    private final SecurityGateUseCase securityGateUseCase;

    // ─── PUBLIC DRIVER PORTAL ENDPOINTS ───────────────────────────────────────

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
}
