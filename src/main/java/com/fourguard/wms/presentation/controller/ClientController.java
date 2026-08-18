package com.fourguard.wms.presentation.controller;

import com.fourguard.wms.application.dto.common.PhysicalDestinationDto;
import com.fourguard.wms.application.dto.request.CreateClientRequest;
import com.fourguard.wms.application.dto.request.UpdateClientRequest;
import com.fourguard.wms.application.dto.response.ClientResponse;
import com.fourguard.wms.application.dto.response.audit.ClientAuditResponse;
import com.fourguard.wms.domain.ports.in.ClientUseCase;
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

/**
 * REST Controller — Gestión de Clientes Depositantes / Owners 3PL.
 * Base path: /api/v1/clients
 */
@RestController
@RequestMapping("/clients")
@RequiredArgsConstructor
@Tag(name = "Clientes", description = "Endpoints para la gestión de Clientes Depositantes 3PL, Matriz de Contactos Corporativos y Direcciones Físicas de Destino (Ship-to Locations)")
public class ClientController {

    private final ClientUseCase clientUseCase;

    // ── CRUD Principal del Cliente ───────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAuthority('CLIENTS_CREATE') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Crear cliente", description = "Registra un nuevo cliente depositante con sus contactos y destinos físicos.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cliente creado con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "RFC / External ID duplicado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes")
    })
    public ResponseEntity<ApiResponse<ClientResponse>> createClient(
            @Valid @RequestBody CreateClientRequest request) {
        ClientResponse response = clientUseCase.createClient(request);
        return ResponseEntity.ok(ApiResponse.ok("Cliente creado con éxito", response));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('CLIENTS_UPDATE') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Actualizar cliente", description = "Actualiza los datos del cliente y sincroniza inteligentemente contactos y destinos.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cliente actualizado con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cliente no encontrado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "RFC / External ID duplicado")
    })
    public ResponseEntity<ApiResponse<ClientResponse>> updateClient(
            @Valid @RequestBody UpdateClientRequest request) {
        ClientResponse response = clientUseCase.updateClient(request);
        return ResponseEntity.ok(ApiResponse.ok("Cliente actualizado con éxito", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CLIENTS_READ') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Obtener cliente por ID", description = "Recupera un cliente con sus contactos y destinos por su UUID.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cliente encontrado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cliente no encontrado")
    })
    public ResponseEntity<ApiResponse<ClientResponse>> getClientById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Cliente encontrado con éxito", clientUseCase.getClientById(id)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CLIENTS_READ') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Listar clientes", description = "Recupera la lista de clientes, opcionalmente filtrada por organización.")
    public ResponseEntity<ApiResponse<List<ClientResponse>>> getClients(
            @RequestParam(required = false) UUID organizationId) {
        List<ClientResponse> response = organizationId != null
                ? clientUseCase.getClientsByOrganizationId(organizationId)
                : clientUseCase.getAllClients();
        return ResponseEntity.ok(ApiResponse.ok("Lista de clientes recuperada con éxito", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CLIENTS_DELETE') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Eliminar cliente", description = "Elimina físicamente un cliente y sus registros relacionados (CASCADE). Usar solo si no tiene historial de movimientos.")
    public ResponseEntity<ApiResponse<Void>> deleteClient(@PathVariable UUID id) {
        clientUseCase.deleteClient(id);
        return ResponseEntity.ok(ApiResponse.ok("Cliente eliminado con éxito"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('CLIENTS_UPDATE') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Alternar estado del cliente", description = "Cambia el estado del cliente entre ACTIVE e INACTIVE (baja lógica - RN-CLI-006).")
    public ResponseEntity<ApiResponse<ClientResponse>> toggleClientStatus(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Estado del cliente actualizado", clientUseCase.toggleClientStatus(id)));
    }

    @GetMapping("/{id}/audit")
    @PreAuthorize("hasAuthority('CLIENTS_READ') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Historial de auditoría del cliente", description = "Recupera la bitácora de cambios para un cliente específico.")
    public ResponseEntity<ApiResponse<List<ClientAuditResponse>>> getClientAuditLogs(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Historial recuperado con éxito", clientUseCase.getClientAuditLogs(id)));
    }

    // ── Endpoints Granulares de Destinos Físicos (Ship-to Locations) ──────────

    @GetMapping("/{id}/destinations")
    @PreAuthorize("hasAuthority('CLIENTS_READ') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Listar destinos del cliente", description = "Recupera todas las bodegas/plantas registradas para un cliente.")
    public ResponseEntity<ApiResponse<List<PhysicalDestinationDto>>> getClientDestinations(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Destinos recuperados con éxito", clientUseCase.getClientDestinations(id)));
    }

    @PostMapping("/{id}/destinations")
    @PreAuthorize("hasAuthority('CLIENTS_UPDATE') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Agregar destino físico", description = "Vincula una nueva bodega/planta de destino a un cliente existente.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Destino agregado con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Código de destino duplicado para este cliente")
    })
    public ResponseEntity<ApiResponse<PhysicalDestinationDto>> addDestination(
            @PathVariable UUID id,
            @Valid @RequestBody PhysicalDestinationDto destinationDto) {
        return ResponseEntity.ok(ApiResponse.ok("Destino físico agregado con éxito",
                clientUseCase.addDestination(id, destinationDto)));
    }

    @PutMapping("/{id}/destinations/{destinationId}")
    @PreAuthorize("hasAuthority('CLIENTS_UPDATE') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Actualizar destino físico", description = "Actualiza los datos de una bodega/planta de destino específica.")
    public ResponseEntity<ApiResponse<PhysicalDestinationDto>> updateDestination(
            @PathVariable UUID id,
            @PathVariable UUID destinationId,
            @Valid @RequestBody PhysicalDestinationDto destinationDto) {
        return ResponseEntity.ok(ApiResponse.ok("Destino físico actualizado con éxito",
                clientUseCase.updateDestination(id, destinationId, destinationDto)));
    }

    @DeleteMapping("/{id}/destinations/{destinationId}")
    @PreAuthorize("hasAuthority('CLIENTS_UPDATE') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Eliminar destino físico", description = "Elimina una bodega/planta de destino de un cliente.")
    public ResponseEntity<ApiResponse<Void>> deleteDestination(
            @PathVariable UUID id,
            @PathVariable UUID destinationId) {
        clientUseCase.deleteDestination(id, destinationId);
        return ResponseEntity.ok(ApiResponse.ok("Destino físico eliminado con éxito"));
    }
}
