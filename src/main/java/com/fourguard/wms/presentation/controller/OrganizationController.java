package com.fourguard.wms.presentation.controller;

import com.fourguard.wms.application.dto.request.CreateOrganizationRequest;
import com.fourguard.wms.application.dto.request.UpdateOrganizationRequest;
import com.fourguard.wms.application.dto.response.OrganizationResponse;
import com.fourguard.wms.application.dto.response.audit.OrganizationAuditResponse;
import com.fourguard.wms.domain.ports.in.CreateOrganizationUseCase;
import com.fourguard.wms.domain.ports.in.DeleteOrganizationUseCase;
import com.fourguard.wms.domain.ports.in.GetOrganizationUseCase;
import com.fourguard.wms.domain.ports.in.UpdateOrganizationUseCase;
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

/** REST controller for Organization Management. */
@RestController
@RequestMapping("/organizations")
@RequiredArgsConstructor
@Tag(name = "Organizaciones", description = "Endpoints para la gestión y administración de organizaciones (Tenants)")
public class OrganizationController {

    private final CreateOrganizationUseCase createOrganizationUseCase;
    private final UpdateOrganizationUseCase updateOrganizationUseCase;
    private final GetOrganizationUseCase getOrganizationUseCase;
    private final DeleteOrganizationUseCase deleteOrganizationUseCase;

    @PostMapping
    @PreAuthorize("hasAuthority('ORGANIZATIONS_CREATE') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Crear organización", description = "Crea una nueva organización en el sistema.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Organización creada con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o duplicados"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes")
    })
    public ResponseEntity<ApiResponse<OrganizationResponse>> createOrganization(@Valid @RequestBody CreateOrganizationRequest request) {
        OrganizationResponse response = createOrganizationUseCase.createOrganization(request);
        return ResponseEntity.ok(ApiResponse.ok("Organización creada con éxito", response));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('ORGANIZATIONS_UPDATE') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Actualizar organización", description = "Actualiza los datos de una organización existente.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Organización actualizada con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Organización no encontrada")
    })
    public ResponseEntity<ApiResponse<OrganizationResponse>> updateOrganization(@Valid @RequestBody UpdateOrganizationRequest request) {
        OrganizationResponse response = updateOrganizationUseCase.updateOrganization(request);
        return ResponseEntity.ok(ApiResponse.ok("Organización actualizada con éxito", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ORGANIZATIONS_READ') or isAuthenticated()")
    @Operation(summary = "Obtener organización por ID", description = "Recupera los detalles de una organización específica por su UUID.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Organización encontrada"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Organización no encontrada")
    })
    public ResponseEntity<ApiResponse<OrganizationResponse>> getOrganizationById(@PathVariable UUID id) {
        OrganizationResponse response = getOrganizationUseCase.getOrganizationById(id);
        return ResponseEntity.ok(ApiResponse.ok("Organización encontrada", response));
    }

    @GetMapping("/{id}/audit")
    @PreAuthorize("hasAuthority('ORGANIZATIONS_READ') or hasAuthority('AUDIT_READ') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Historial de auditoría de la organización", description = "Devuelve el historial cronológico de cambios de una organización específica.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Historial de auditoría recuperado con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Organización no encontrada")
    })
    public ResponseEntity<ApiResponse<List<OrganizationAuditResponse>>> getOrganizationAuditLogs(@PathVariable UUID id) {
        List<OrganizationAuditResponse> response = getOrganizationUseCase.getOrganizationAuditLogs(id);
        return ResponseEntity.ok(ApiResponse.ok("Historial de auditoría recuperado con éxito", response));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ORGANIZATIONS_READ') or isAuthenticated()")
    @Operation(summary = "Obtener todas las organizaciones", description = "Recupera la lista de todas las organizaciones registradas.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lista recuperada con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes")
    })
    public ResponseEntity<ApiResponse<List<OrganizationResponse>>> getAllOrganizations() {
        List<OrganizationResponse> response = getOrganizationUseCase.getAllOrganizations();
        return ResponseEntity.ok(ApiResponse.ok("Lista de organizaciones recuperada con éxito", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ORGANIZATIONS_DELETE') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Eliminar organización", description = "Elimina físicamente una organización del sistema por su ID.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Organización eliminada con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Organización no encontrada")
    })
    public ResponseEntity<ApiResponse<Void>> deleteOrganization(@PathVariable UUID id) {
        deleteOrganizationUseCase.deleteOrganization(id);
        return ResponseEntity.ok(ApiResponse.ok("Organización eliminada con éxito"));
    }
}
