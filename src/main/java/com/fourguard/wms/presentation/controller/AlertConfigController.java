package com.fourguard.wms.presentation.controller;

import com.fourguard.wms.application.dto.request.AlertConfigFilterRequest;
import com.fourguard.wms.application.dto.request.CreateAlertConfigRequest;
import com.fourguard.wms.application.dto.request.UpdateAlertConfigRequest;
import com.fourguard.wms.application.dto.request.UpdateAlertConfigStatusRequest;
import com.fourguard.wms.application.dto.response.AlertConfigResponse;
import com.fourguard.wms.application.dto.response.audit.AlertConfigAuditResponse;
import com.fourguard.wms.domain.enums.AlertCategory;
import com.fourguard.wms.domain.enums.AlertEvent;
import com.fourguard.wms.domain.enums.AlertPriority;
import com.fourguard.wms.domain.enums.AlertStatus;
import com.fourguard.wms.domain.exception.ValidationException;
import com.fourguard.wms.domain.ports.in.AlertConfigUseCase;
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

/** REST controller for Notification & Alert Configurations (HU-134). */
@RestController
@RequestMapping("/alerts-config")
@RequiredArgsConstructor
@Tag(name = "Configuración de Alertas", description = "Endpoints para la gestión y definición de reglas de notificaciones operativas (HU-134)")
public class AlertConfigController {

    private final AlertConfigUseCase alertConfigUseCase;

    @PostMapping
    @PreAuthorize("hasAuthority('ALERTS_WRITE') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Registrar nueva regla de alerta", description = "Crea una nueva regla de alerta y configuración de notificaciones para la organización.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Regla de alerta creada con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o nombre duplicado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes")
    })
    public ResponseEntity<ApiResponse<AlertConfigResponse>> createAlertConfig(@Valid @RequestBody CreateAlertConfigRequest request) {
        AlertConfigResponse response = alertConfigUseCase.createAlertConfig(request);
        return ResponseEntity.ok(ApiResponse.ok("Regla de alerta creada con éxito", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ALERTS_WRITE') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Actualizar regla de alerta", description = "Modifica los parámetros y condiciones de una regla de alerta existente por su ID.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Regla de alerta actualizada con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o nombre duplicado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Regla no encontrada")
    })
    public ResponseEntity<ApiResponse<AlertConfigResponse>> updateAlertConfig(@PathVariable UUID id, @Valid @RequestBody UpdateAlertConfigRequest request) {
        if (request.getId() == null) {
            request.setId(id);
        } else if (!id.equals(request.getId())) {
            throw new ValidationException("El ID del path no coincide con el ID del cuerpo de la solicitud");
        }
        AlertConfigResponse response = alertConfigUseCase.updateAlertConfig(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Regla de alerta actualizada con éxito", response));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ALERTS_WRITE') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Cambiar estatus de regla de alerta", description = "Activa o inactiva una regla de alerta por su ID.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Estatus de regla actualizado con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Regla no encontrada")
    })
    public ResponseEntity<ApiResponse<AlertConfigResponse>> updateAlertConfigStatus(@PathVariable UUID id, @Valid @RequestBody UpdateAlertConfigStatusRequest request) {
        AlertConfigResponse response = alertConfigUseCase.updateAlertConfigStatus(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Estatus de la regla de alerta actualizado con éxito", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ALERTS_READ') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Consultar regla de alerta por ID", description = "Obtiene el detalle completo de una regla de alerta configurada.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Regla de alerta encontrada con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Regla no encontrada")
    })
    public ResponseEntity<ApiResponse<AlertConfigResponse>> getAlertConfigById(@PathVariable UUID id) {
        AlertConfigResponse response = alertConfigUseCase.getAlertConfigById(id);
        return ResponseEntity.ok(ApiResponse.ok("Regla de alerta encontrada con éxito", response));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ALERTS_READ') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Listar reglas de alertas", description = "Recupera la lista de reglas de alertas de la organización con opción de filtrado.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lista de reglas recuperada con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes")
    })
    public ResponseEntity<ApiResponse<List<AlertConfigResponse>>> getAlertConfigs(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) AlertCategory category,
            @RequestParam(required = false) AlertEvent event,
            @RequestParam(required = false) AlertPriority priority,
            @RequestParam(required = false) AlertStatus status,
            @RequestParam(required = false) String search) {

        AlertConfigFilterRequest filter = AlertConfigFilterRequest.builder()
                .organizationId(organizationId)
                .category(category)
                .event(event)
                .priority(priority)
                .status(status)
                .search(search)
                .build();

        List<AlertConfigResponse> response = alertConfigUseCase.getAlertConfigs(filter);
        return ResponseEntity.ok(ApiResponse.ok("Lista de reglas de alerta recuperada con éxito", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ALERTS_WRITE') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Archivar regla de alerta (Soft Delete)", description = "Marca una regla de alerta como eliminada e inactiva conservando su historial.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Regla de alerta archivada con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Regla no encontrada")
    })
    public ResponseEntity<ApiResponse<Void>> deleteAlertConfig(@PathVariable UUID id) {
        alertConfigUseCase.deleteAlertConfig(id);
        return ResponseEntity.ok(ApiResponse.ok("Regla de alerta archivada con éxito"));
    }

    @GetMapping("/{id}/audit")
    @PreAuthorize("hasAuthority('AUDIT_READ') or hasAuthority('ALERTS_READ') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Consultar historial de auditoría de la regla", description = "Obtiene la bitácora inmutable de modificaciones asociadas a una regla de alerta.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Historial de auditoría recuperado con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Regla no encontrada")
    })
    public ResponseEntity<ApiResponse<List<AlertConfigAuditResponse>>> getAlertConfigAuditLogs(@PathVariable UUID id) {
        List<AlertConfigAuditResponse> response = alertConfigUseCase.getAlertConfigAuditLogs(id);
        return ResponseEntity.ok(ApiResponse.ok("Historial de auditoría recuperado con éxito", response));
    }
}
