package com.fourguard.wms.presentation.controller;

import com.fourguard.wms.application.dto.request.*;
import com.fourguard.wms.application.dto.response.*;
import com.fourguard.wms.domain.ports.in.WmsLicenseUseCase;
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

/** REST controller for WMS License Management. */
@RestController
@RequestMapping("/licenses")
@RequiredArgsConstructor
@Tag(name = "Licencias WMS", description = "Endpoints para la gestión, emisión, actualización, renovación y auditoría de licencias WMS")
public class WmsLicenseController {

    private final WmsLicenseUseCase wmsLicenseUseCase;

    @GetMapping
    @PreAuthorize("hasAuthority('LICENSES_READ') or hasRole('ADMIN') or hasRole('OPS') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Listar todas las licencias WMS", description = "Obtiene la lista de licencias registradas (filtrable opcionalmente por organización).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lista de licencias obtenida con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    public ResponseEntity<ApiResponse<List<WmsLicenseResponse>>> getAllLicenses(@RequestParam(required = false) UUID organizationId) {
        List<WmsLicenseResponse> response = wmsLicenseUseCase.getAllLicenses(organizationId);
        return ResponseEntity.ok(ApiResponse.ok("Lista de licencias obtenida con éxito", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('LICENSES_READ') or hasRole('ADMIN') or hasRole('OPS') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Obtener detalle completo de licencia", description = "Recupera la licencia y sus consumos en tiempo real por su ID.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Detalle de licencia obtenido con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Licencia no encontrada")
    })
    public ResponseEntity<ApiResponse<LicenseDetailResponse>> getLicenseById(@PathVariable UUID id) {
        LicenseDetailResponse response = wmsLicenseUseCase.getLicenseById(id);
        return ResponseEntity.ok(ApiResponse.ok("Detalle de licencia obtenido con éxito", response));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('LICENSES_WRITE') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Emitir nueva licencia WMS", description = "Registra una nueva licencia, genera su clave secreta y la asigna a la organización.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Licencia emitida con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Acceso denegado")
    })
    public ResponseEntity<ApiResponse<WmsLicenseResponse>> createLicense(@Valid @RequestBody CreateLicenseRequest request) {
        WmsLicenseResponse response = wmsLicenseUseCase.createLicense(request);
        return ResponseEntity.ok(ApiResponse.ok("Licencia emitida con éxito", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('LICENSES_WRITE') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Modificar licencia WMS", description = "Modifica capacidades, módulos o metadatos de la licencia conservando su historial.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Licencia actualizada con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Solicitud inválida"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Licencia no encontrada")
    })
    public ResponseEntity<ApiResponse<WmsLicenseResponse>> updateLicense(@PathVariable UUID id, @Valid @RequestBody UpdateLicenseRequest request) {
        WmsLicenseResponse response = wmsLicenseUseCase.updateLicense(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Licencia actualizada con éxito", response));
    }

    @PostMapping("/{id}/renew")
    @PreAuthorize("hasAuthority('LICENSES_MANAGE') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Renovar licencia WMS", description = "Extiende la vigencia contractual y plan de la licencia.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Licencia renovada con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Fecha inválida"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Licencia no encontrada")
    })
    public ResponseEntity<ApiResponse<WmsLicenseResponse>> renewLicense(@PathVariable UUID id, @Valid @RequestBody RenewLicenseRequest request) {
        WmsLicenseResponse response = wmsLicenseUseCase.renewLicense(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Licencia renovada con éxito", response));
    }

    @PostMapping("/{id}/suspend")
    @PreAuthorize("hasAuthority('LICENSES_MANAGE') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Suspender licencia WMS", description = "Suspende administrativamente una licencia por falta de pago u otro motivo.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Licencia suspendida con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Licencia no encontrada")
    })
    public ResponseEntity<ApiResponse<WmsLicenseResponse>> suspendLicense(@PathVariable UUID id, @Valid @RequestBody SuspendLicenseRequest request) {
        WmsLicenseResponse response = wmsLicenseUseCase.suspendLicense(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Licencia suspendida con éxito", response));
    }

    @PostMapping("/{id}/reactivate")
    @PreAuthorize("hasAuthority('LICENSES_MANAGE') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Reactivar licencia WMS", description = "Reactiva una licencia previamente suspendida.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Licencia reactivada con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Licencia no encontrada")
    })
    public ResponseEntity<ApiResponse<WmsLicenseResponse>> reactivateLicense(@PathVariable UUID id) {
        WmsLicenseResponse response = wmsLicenseUseCase.reactivateLicense(id);
        return ResponseEntity.ok(ApiResponse.ok("Licencia reactivada con éxito", response));
    }

    @PostMapping("/{id}/revoke")
    @PreAuthorize("hasAuthority('LICENSES_MANAGE') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Revocar licencia WMS", description = "Revoca permanentemente una licencia impidiendo su uso futuro.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Licencia revocada con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Licencia no encontrada")
    })
    public ResponseEntity<ApiResponse<WmsLicenseResponse>> revokeLicense(@PathVariable UUID id, @RequestParam(required = false) String reason) {
        WmsLicenseResponse response = wmsLicenseUseCase.revokeLicense(id, reason);
        return ResponseEntity.ok(ApiResponse.ok("Licencia revocada con éxito", response));
    }

    @PostMapping("/{id}/regenerate-key")
    @PreAuthorize("hasAuthority('LICENSES_MANAGE') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Regenerar clave secreta de licencia", description = "Genera y re-encripta una nueva clave secreta para la licencia.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Clave de licencia regenerada con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Licencia no encontrada")
    })
    public ResponseEntity<ApiResponse<LicenseKeyGeneratedResponse>> regenerateKey(@PathVariable UUID id) {
        LicenseKeyGeneratedResponse response = wmsLicenseUseCase.regenerateKey(id);
        return ResponseEntity.ok(ApiResponse.ok("Clave de licencia regenerada con éxito", response));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAuthority('LICENSES_READ') or hasAuthority('AUDIT_READ') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Consultar bitácora de historial de la licencia", description = "Obtiene la bitácora inmutable de cambios y renovaciones de una licencia.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Historial de la licencia obtenido con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Licencia no encontrada")
    })
    public ResponseEntity<ApiResponse<List<WmsLicenseHistoryResponse>>> getLicenseHistory(@PathVariable UUID id) {
        List<WmsLicenseHistoryResponse> response = wmsLicenseUseCase.getLicenseHistory(id);
        return ResponseEntity.ok(ApiResponse.ok("Historial de la licencia obtenido con éxito", response));
    }
}
