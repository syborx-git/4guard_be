package com.fourguard.wms.presentation.controller;

import com.fourguard.wms.application.dto.request.CreateLocationRequest;
import com.fourguard.wms.application.dto.request.UpdateLocationRequest;
import com.fourguard.wms.application.dto.request.UpdateLocationStatusRequest;
import com.fourguard.wms.application.dto.response.LocationResponse;
import com.fourguard.wms.application.dto.response.audit.LocationAuditResponse;
import com.fourguard.wms.domain.ports.in.LocationUseCase;
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

/** REST controller for Location Management. */
@RestController
@RequestMapping("/locations")
@RequiredArgsConstructor
@Tag(name = "Ubicaciones", description = "Endpoints para la gestión y administración de las ubicaciones físicas en el almacén")
public class LocationController {

    private final LocationUseCase locationUseCase;

    // =========================================================================
    // POST — Create
    // =========================================================================

    @PostMapping
    @PreAuthorize("hasAuthority('LOCATIONS_CREATE') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Crear ubicación", description = "Registra una nueva posición de almacenamiento física con estado inicial ACTIVE.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Ubicación creada con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes")
    })
    public ResponseEntity<ApiResponse<LocationResponse>> createLocation(@Valid @RequestBody CreateLocationRequest request) {
        LocationResponse response = locationUseCase.createLocation(request);
        return ResponseEntity.ok(ApiResponse.ok("Ubicación creada con éxito", response));
    }

    // =========================================================================
    // PUT — Update data (NOT status)
    // =========================================================================

    @PutMapping
    @PreAuthorize("hasAuthority('LOCATIONS_UPDATE') or hasRole('OPERATIONS_MANAGER')")
    @Operation(
        summary = "Actualizar ubicación",
        description = "Actualiza los datos de una ubicación. Para cambiar el estado operativo usa PATCH /{id}/status."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Ubicación actualizada con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Ubicación no encontrada"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "El código ya está asignado a otra ubicación")
    })
    public ResponseEntity<ApiResponse<LocationResponse>> updateLocation(@Valid @RequestBody UpdateLocationRequest request) {
        LocationResponse response = locationUseCase.updateLocation(request);
        return ResponseEntity.ok(ApiResponse.ok("Ubicación actualizada con éxito", response));
    }

    // =========================================================================
    // PATCH — FSM status change
    // =========================================================================

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('LOCATIONS_UPDATE') or hasRole('OPERATIONS_MANAGER')")
    @Operation(
        summary = "Cambiar estado de la ubicación",
        description = """
            Cambia el estado operativo (FSM) de una ubicación.
            Transiciones permitidas:
              ACTIVE → BLOCKED | MAINTENANCE | INACTIVE
              BLOCKED → ACTIVE
              MAINTENANCE → ACTIVE
              INACTIVE → ACTIVE
            El campo 'reason' es obligatorio para BLOCKED y MAINTENANCE.
            Solo se permite INACTIVE si currentOccupancy = 0.
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Estado cambiado con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Reason obligatorio no proporcionado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Ubicación no encontrada"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Ubicación con inventario activo (para INACTIVE)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Transición de estado inválida")
    })
    public ResponseEntity<ApiResponse<LocationResponse>> changeLocationStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateLocationStatusRequest request) {
        LocationResponse response = locationUseCase.changeLocationStatus(id, request);
        String msg = "Estado cambiado a " + request.getStatus().name() + " correctamente.";
        return ResponseEntity.ok(ApiResponse.ok(msg, response));
    }

    // =========================================================================
    // GET — Read
    // =========================================================================

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('LOCATIONS_READ') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Obtener ubicación por ID", description = "Recupera los detalles de una ubicación específica por su UUID.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Ubicación encontrada con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Ubicación no encontrada")
    })
    public ResponseEntity<ApiResponse<LocationResponse>> getLocationById(@PathVariable UUID id) {
        LocationResponse response = locationUseCase.getLocationById(id);
        return ResponseEntity.ok(ApiResponse.ok("Ubicación encontrada con éxito", response));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('LOCATIONS_READ') or hasRole('OPERATIONS_MANAGER')")
    @Operation(
        summary = "Obtener ubicaciones",
        description = "Recupera la lista de ubicaciones, opcionalmente filtrando por sucursal y estado de disponibilidad."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lista de ubicaciones recuperada con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes")
    })
    public ResponseEntity<ApiResponse<List<LocationResponse>>> getLocations(
            @RequestParam(required = false) UUID branchId,
            @RequestParam(defaultValue = "false") boolean availableOnly) {
        List<LocationResponse> response;
        if (branchId != null) {
            if (availableOnly) {
                response = locationUseCase.getAvailableLocationsByBranchId(branchId);
            } else {
                response = locationUseCase.getLocationsByBranchId(branchId);
            }
        } else {
            response = locationUseCase.getAllLocations();
        }
        return ResponseEntity.ok(ApiResponse.ok("Lista de ubicaciones recuperada con éxito", response));
    }

    // =========================================================================
    // DELETE
    // =========================================================================

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('LOCATIONS_DELETE') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Eliminar ubicación", description = "Elimina físicamente una ubicación del sistema por su ID.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Ubicación eliminada con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Ubicación no encontrada")
    })
    public ResponseEntity<ApiResponse<Void>> deleteLocation(@PathVariable UUID id) {
        locationUseCase.deleteLocation(id);
        return ResponseEntity.ok(ApiResponse.ok("Ubicación eliminada con éxito"));
    }

    // =========================================================================
    // AUDIT LOGS
    // =========================================================================

    @GetMapping("/{id}/audit")
    @PreAuthorize("hasAuthority('LOCATIONS_READ') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Historial de auditoría de la ubicación", description = "Devuelve el historial cronológico de cambios de una ubicación específica.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Historial de auditoría recuperado con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Ubicación no encontrada")
    })
    public ResponseEntity<ApiResponse<List<LocationAuditResponse>>> getLocationAuditLogs(@PathVariable UUID id) {
        List<LocationAuditResponse> response = locationUseCase.getLocationAuditLogs(id);
        return ResponseEntity.ok(ApiResponse.ok("Historial de auditoría recuperado con éxito", response));
    }

    // =========================================================================
    // BAY OCCUPANCY & SUGGESTION (22-PALLET STANDARD)
    // =========================================================================

    @GetMapping("/bays/occupancy")
    @PreAuthorize("hasAuthority('LOCATIONS_READ') or hasRole('OPERATIONS_MANAGER') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('WAREHOUSE_SUPERVISOR')")
    @Operation(summary = "Matriz de ocupación y semáforo de bahías",
               description = "Calcula el porcentaje de ocupación con estándar de 22 pallets y semáforo verde/ámbar/rojo.")
    public ResponseEntity<ApiResponse<List<com.fourguard.wms.application.dto.response.BayOccupancyResponse>>> getBayOccupancy(
            @RequestParam(required = false) UUID branchId) {
        List<com.fourguard.wms.application.dto.response.BayOccupancyResponse> response = locationUseCase.getBayOccupancyList(branchId);
        return ResponseEntity.ok(ApiResponse.ok("Matriz de ocupación de bahías recuperada con éxito", response));
    }
}
