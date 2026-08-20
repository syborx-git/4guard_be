package com.fourguard.wms.presentation.controller;

import com.fourguard.wms.application.dto.request.CreateShiftRequest;
import com.fourguard.wms.application.dto.request.ShiftFilterRequest;
import com.fourguard.wms.application.dto.request.UpdateShiftRequest;
import com.fourguard.wms.application.dto.request.UpdateShiftStatusRequest;
import com.fourguard.wms.application.dto.response.ShiftResponse;
import com.fourguard.wms.application.dto.response.audit.ShiftAuditResponse;
import com.fourguard.wms.domain.enums.ShiftScopeType;
import com.fourguard.wms.domain.enums.ShiftStatus;
import com.fourguard.wms.domain.ports.in.ShiftUseCase;
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

/** REST controller for Shift and Schedule Management (HU-140). */
@RestController
@RequestMapping("/shifts")
@RequiredArgsConstructor
@Tag(name = "Turnos y Horarios", description = "Endpoints para la gestión y configuración de turnos y jornadas operativas (HU-140)")
public class ShiftController {

    private final ShiftUseCase shiftUseCase;

    @PostMapping
    @PreAuthorize("hasAuthority('SHIFTS_CREATE') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Crear turno", description = "Registra un nuevo turno u horario operativo en el sistema.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Turno creado con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o solapamiento de horarios"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes")
    })
    public ResponseEntity<ApiResponse<ShiftResponse>> createShift(@Valid @RequestBody CreateShiftRequest request) {
        ShiftResponse response = shiftUseCase.createShift(request);
        return ResponseEntity.ok(ApiResponse.ok("Turno creado con éxito", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SHIFTS_UPDATE') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Actualizar turno", description = "Actualiza los datos generales y días operativos de un turno existente.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Turno actualizado con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o solapamiento de horarios"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Turno no encontrado")
    })
    public ResponseEntity<ApiResponse<ShiftResponse>> updateShift(@PathVariable UUID id, @Valid @RequestBody UpdateShiftRequest request) {
        if (!id.equals(request.getId())) {
            throw new com.fourguard.wms.domain.exception.ValidationException("El ID del path no coincide con el ID del cuerpo");
        }
        ShiftResponse response = shiftUseCase.updateShift(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Turno actualizado con éxito", response));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('SHIFTS_STATUS_CHANGE') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Cambiar estatus de turno", description = "Cambia el estado operativo de un turno entre ACTIVE e INACTIVE.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Estatus de turno actualizado con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Turno no encontrado")
    })
    public ResponseEntity<ApiResponse<ShiftResponse>> updateShiftStatus(@PathVariable UUID id, @Valid @RequestBody UpdateShiftStatusRequest request) {
        ShiftResponse response = shiftUseCase.updateShiftStatus(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Estatus de turno actualizado con éxito", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SHIFTS_READ') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Obtener turno por ID", description = "Recupera la información detallada de un turno por su UUID.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Turno encontrado con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Turno no encontrado")
    })
    public ResponseEntity<ApiResponse<ShiftResponse>> getShiftById(@PathVariable UUID id) {
        ShiftResponse response = shiftUseCase.getShiftById(id);
        return ResponseEntity.ok(ApiResponse.ok("Turno encontrado con éxito", response));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SHIFTS_READ') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Obtener lista de turnos", description = "Recupera la lista de turnos registrados filtrando por sucursal, estatus, día u organización.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lista de turnos recuperada con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes")
    })
    public ResponseEntity<ApiResponse<List<ShiftResponse>>> getShifts(
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false) UUID warehouseSectionId,
            @RequestParam(required = false) ShiftStatus status,
            @RequestParam(required = false) ShiftScopeType scopeType,
            @RequestParam(required = false) String dayOfWeek,
            @RequestParam(required = false) String search) {

        ShiftFilterRequest filter = ShiftFilterRequest.builder()
                .branchId(branchId)
                .warehouseSectionId(warehouseSectionId)
                .status(status)
                .scopeType(scopeType)
                .dayOfWeek(dayOfWeek)
                .search(search)
                .build();

        List<ShiftResponse> response = shiftUseCase.getShifts(filter);
        return ResponseEntity.ok(ApiResponse.ok("Lista de turnos recuperada con éxito", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SHIFTS_DELETE') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Eliminar turno", description = "Realiza un borrado lógico del turno estableciendo is_deleted=true e inactivo.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Turno eliminado con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Turno no encontrado")
    })
    public ResponseEntity<ApiResponse<Void>> deleteShift(@PathVariable UUID id) {
        shiftUseCase.deleteShift(id);
        return ResponseEntity.ok(ApiResponse.ok("Turno eliminado con éxito"));
    }

    @GetMapping("/{id}/audit")
    @PreAuthorize("hasAuthority('SHIFTS_READ') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Obtener historial de auditoría del turno", description = "Recupera la bitácora de auditoría y cambios para un turno por su UUID.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Historial de auditoría recuperado con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Turno no encontrado")
    })
    public ResponseEntity<ApiResponse<List<ShiftAuditResponse>>> getShiftAuditLogs(@PathVariable UUID id) {
        List<ShiftAuditResponse> response = shiftUseCase.getShiftAuditLogs(id);
        return ResponseEntity.ok(ApiResponse.ok("Historial de auditoría recuperado con éxito", response));
    }
}
