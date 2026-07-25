package com.fourguard.wms.presentation.controller;

import com.fourguard.wms.application.dto.request.CreateClientRequest;
import com.fourguard.wms.application.dto.request.UpdateClientRequest;
import com.fourguard.wms.application.dto.response.ClientResponse;
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

/** REST controller for Client Management. */
@RestController
@RequestMapping("/clients")
@RequiredArgsConstructor
@Tag(name = "Clientes", description = "Endpoints para la gestión y administración de clientes (Owners de inventario)")
public class ClientController {

    private final ClientUseCase clientUseCase;

    @PostMapping
    @PreAuthorize("hasAuthority('CLIENTS_CREATE') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Crear cliente", description = "Registra un nuevo cliente depositante en el WMS.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cliente creado con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes")
    })
    public ResponseEntity<ApiResponse<ClientResponse>> createClient(@Valid @RequestBody CreateClientRequest request) {
        ClientResponse response = clientUseCase.createClient(request);
        return ResponseEntity.ok(ApiResponse.ok("Cliente creado con éxito", response));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('CLIENTS_UPDATE') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Actualizar cliente", description = "Actualiza los datos de un cliente existente.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cliente actualizado con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cliente no encontrado")
    })
    public ResponseEntity<ApiResponse<ClientResponse>> updateClient(@Valid @RequestBody UpdateClientRequest request) {
        ClientResponse response = clientUseCase.updateClient(request);
        return ResponseEntity.ok(ApiResponse.ok("Cliente actualizado con éxito", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CLIENTS_READ') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Obtener cliente por ID", description = "Recupera los detalles de un cliente específico por su UUID.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cliente encontrado con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cliente no encontrado")
    })
    public ResponseEntity<ApiResponse<ClientResponse>> getClientById(@PathVariable UUID id) {
        ClientResponse response = clientUseCase.getClientById(id);
        return ResponseEntity.ok(ApiResponse.ok("Cliente encontrado con éxito", response));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CLIENTS_READ') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Obtener clientes", description = "Recupera la lista de clientes, opcionalmente filtrando por organización.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lista de clientes recuperada con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes")
    })
    public ResponseEntity<ApiResponse<List<ClientResponse>>> getClients(@RequestParam(required = false) UUID organizationId) {
        List<ClientResponse> response;
        if (organizationId != null) {
            response = clientUseCase.getClientsByOrganizationId(organizationId);
        } else {
            response = clientUseCase.getAllClients();
        }
        return ResponseEntity.ok(ApiResponse.ok("Lista de clientes recuperada con éxito", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CLIENTS_DELETE') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Eliminar cliente", description = "Elimina físicamente un cliente del sistema por su ID.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cliente eliminado con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cliente no encontrado")
    })
    public ResponseEntity<ApiResponse<Void>> deleteClient(@PathVariable UUID id) {
        clientUseCase.deleteClient(id);
        return ResponseEntity.ok(ApiResponse.ok("Cliente eliminado con éxito"));
    }

    @GetMapping("/{id}/audit")
    @PreAuthorize("hasAuthority('CLIENTS_READ') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Obtener historial de auditoría de cliente", description = "Recupera la bitácora de cambios para un cliente específico por su UUID.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Historial de auditoría recuperado con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cliente no encontrado")
    })
    public ResponseEntity<ApiResponse<List<com.fourguard.wms.application.dto.response.audit.ClientAuditResponse>>> getClientAuditLogs(@PathVariable UUID id) {
        List<com.fourguard.wms.application.dto.response.audit.ClientAuditResponse> response = clientUseCase.getClientAuditLogs(id);
        return ResponseEntity.ok(ApiResponse.ok("Historial de auditoría recuperado con éxito", response));
    }
}
