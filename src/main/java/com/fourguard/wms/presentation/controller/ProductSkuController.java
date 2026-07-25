package com.fourguard.wms.presentation.controller;

import com.fourguard.wms.application.dto.request.CreateProductSkuRequest;
import com.fourguard.wms.application.dto.request.UpdateProductSkuRequest;
import com.fourguard.wms.application.dto.response.ProductSkuResponse;
import com.fourguard.wms.domain.ports.in.ProductSkuUseCase;
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

/** REST controller for Product SKU Management. */
@RestController
@RequestMapping("/product-skus")
@RequiredArgsConstructor
@Tag(name = "SKUs / Productos", description = "Endpoints para la gestión y administración del catálogo de SKUs de los clientes")
public class ProductSkuController {

    private final ProductSkuUseCase productSkuUseCase;

    @PostMapping
    @PreAuthorize("hasAuthority('INVENTORY_CREATE') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Crear SKU", description = "Registra una nueva referencia o SKU en el catálogo de un cliente.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "SKU creado con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes")
    })
    public ResponseEntity<ApiResponse<ProductSkuResponse>> createProductSku(@Valid @RequestBody CreateProductSkuRequest request) {
        ProductSkuResponse response = productSkuUseCase.createProductSku(request);
        return ResponseEntity.ok(ApiResponse.ok("SKU creado con éxito", response));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('INVENTORY_UPDATE') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Actualizar SKU", description = "Actualiza los datos de un SKU existente en el catálogo.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "SKU actualizado con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "SKU no encontrado")
    })
    public ResponseEntity<ApiResponse<ProductSkuResponse>> updateProductSku(@Valid @RequestBody UpdateProductSkuRequest request) {
        ProductSkuResponse response = productSkuUseCase.updateProductSku(request);
        return ResponseEntity.ok(ApiResponse.ok("SKU actualizado con éxito", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('INVENTORY_READ') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Obtener SKU por ID", description = "Recupera los detalles de un SKU específico por su UUID.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "SKU encontrado con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "SKU no encontrado")
    })
    public ResponseEntity<ApiResponse<ProductSkuResponse>> getProductSkuById(@PathVariable UUID id) {
        ProductSkuResponse response = productSkuUseCase.getProductSkuById(id);
        return ResponseEntity.ok(ApiResponse.ok("SKU encontrado con éxito", response));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('INVENTORY_READ') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Obtener SKUs", description = "Recupera la lista de SKUs, opcionalmente filtrada por cliente.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lista de SKUs recuperada con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes")
    })
    public ResponseEntity<ApiResponse<List<ProductSkuResponse>>> getProductSkus(@RequestParam(required = false) UUID clientId) {
        List<ProductSkuResponse> response;
        if (clientId != null) {
            response = productSkuUseCase.getProductSkusByClientId(clientId);
        } else {
            response = productSkuUseCase.getAllProductSkus();
        }
        return ResponseEntity.ok(ApiResponse.ok("Lista de SKUs recuperada con éxito", response));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('INVENTORY_UPDATE') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Cambiar estatus de SKU", description = "Actualiza el estatus de un SKU entre ACTIVE e INACTIVE por su ID.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Estatus de SKU actualizado con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Estatus no válido"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "SKU no encontrado")
    })
    public ResponseEntity<ApiResponse<ProductSkuResponse>> updateProductSkuStatus(
            @PathVariable UUID id,
            @RequestParam String status) {
        ProductSkuResponse response = productSkuUseCase.updateProductSkuStatus(id, status);
        return ResponseEntity.ok(ApiResponse.ok("Estatus de SKU actualizado con éxito", response));
    }

    @PatchMapping("/{id}/soft-delete")
    @PreAuthorize("hasAuthority('INVENTORY_DELETE') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Borrado lógico de SKU", description = "Marca un SKU como eliminado lógicamente (isDeleted = true) y desactiva su estatus.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "SKU eliminado lógicamente con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "SKU no encontrado")
    })
    public ResponseEntity<ApiResponse<Void>> softDeleteProductSku(@PathVariable UUID id) {
        productSkuUseCase.softDeleteProductSku(id);
        return ResponseEntity.ok(ApiResponse.ok("SKU eliminado lógicamente con éxito"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('INVENTORY_DELETE') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Eliminar SKU", description = "Elimina físicamente un SKU del catálogo por su ID.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "SKU eliminado con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "SKU no encontrado")
    })
    public ResponseEntity<ApiResponse<Void>> deleteProductSku(@PathVariable UUID id) {
        productSkuUseCase.deleteProductSku(id);
        return ResponseEntity.ok(ApiResponse.ok("SKU eliminado con éxito"));
    }

    @GetMapping("/{id}/audit")
    @PreAuthorize("hasAuthority('INVENTORY_READ') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Obtener historial de auditoría de producto/SKU", description = "Recupera la bitácora de cambios para un producto/SKU específico por su UUID.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Historial de auditoría recuperado con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "SKU no encontrado")
    })
    public ResponseEntity<ApiResponse<List<com.fourguard.wms.application.dto.response.audit.ProductSkuAuditResponse>>> getProductSkuAuditLogs(@PathVariable UUID id) {
        List<com.fourguard.wms.application.dto.response.audit.ProductSkuAuditResponse> response = productSkuUseCase.getProductSkuAuditLogs(id);
        return ResponseEntity.ok(ApiResponse.ok("Historial de auditoría recuperado con éxito", response));
    }
}
