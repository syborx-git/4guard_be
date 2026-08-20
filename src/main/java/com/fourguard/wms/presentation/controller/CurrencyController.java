package com.fourguard.wms.presentation.controller;

import com.fourguard.wms.application.dto.request.CreateCurrencyRequest;
import com.fourguard.wms.application.dto.request.UpdateCurrencyRequest;
import com.fourguard.wms.application.dto.request.UpdateCurrencyStatusRequest;
import com.fourguard.wms.application.dto.response.CurrencyResponse;
import com.fourguard.wms.domain.ports.in.CurrencyUseCase;
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
 * REST Controller para el Catálogo de Monedas / Divisas por Organización.
 * Base path: /currencies
 */
@RestController
@RequestMapping("/currencies")
@RequiredArgsConstructor
@Tag(name = "Monedas y Divisas", description = "Endpoints para la gestión del catálogo maestro de divisas multi-tenant")
public class CurrencyController {

    private final CurrencyUseCase currencyUseCase;

    @GetMapping
    @PreAuthorize("hasAuthority('CURRENCIES_READ') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Listar divisas", description = "Obtiene la lista de divisas registradas (filtrable opcionalmente por organización).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lista de divisas obtenida con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Permisos insuficientes")
    })
    public ResponseEntity<ApiResponse<List<CurrencyResponse>>> getCurrencies(@RequestParam(required = false) UUID organizationId) {
        List<CurrencyResponse> response = currencyUseCase.getCurrencies(organizationId);
        return ResponseEntity.ok(ApiResponse.ok("Lista de divisas obtenida con éxito", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CURRENCIES_READ') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Obtener detalle de divisa", description = "Recupera la información completa de una divisa por su UUID.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Detalle de divisa obtenido con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Divisa no encontrada")
    })
    public ResponseEntity<ApiResponse<CurrencyResponse>> getCurrencyById(@PathVariable UUID id) {
        CurrencyResponse response = currencyUseCase.getCurrencyById(id);
        return ResponseEntity.ok(ApiResponse.ok("Detalle de divisa obtenido con éxito", response));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CURRENCIES_WRITE') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Crear nueva divisa", description = "Registra una nueva divisa para la organización especificada.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Divisa creada con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Código de divisa ya existente")
    })
    public ResponseEntity<ApiResponse<CurrencyResponse>> createCurrency(@Valid @RequestBody CreateCurrencyRequest request) {
        CurrencyResponse response = currencyUseCase.createCurrency(request);
        return ResponseEntity.ok(ApiResponse.ok("Divisa creada con éxito", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CURRENCIES_WRITE') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Modificar metadatos de divisa", description = "Actualiza el nombre, símbolo y número de decimales de una divisa.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Divisa actualizada con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Divisa no encontrada")
    })
    public ResponseEntity<ApiResponse<CurrencyResponse>> updateCurrency(@PathVariable UUID id, @Valid @RequestBody UpdateCurrencyRequest request) {
        CurrencyResponse response = currencyUseCase.updateCurrency(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Divisa actualizada con éxito", response));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('CURRENCIES_WRITE') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Activar / Inactivar divisa", description = "Cambia el estado de la divisa. No se permite inactivar la divisa base.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Estatus de divisa actualizado con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "No se puede inactivar la divisa base")
    })
    public ResponseEntity<ApiResponse<CurrencyResponse>> updateCurrencyStatus(@PathVariable UUID id, @Valid @RequestBody UpdateCurrencyStatusRequest request) {
        CurrencyResponse response = currencyUseCase.updateCurrencyStatus(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Estatus de divisa actualizado con éxito", response));
    }

    @PostMapping("/{id}/set-base")
    @PreAuthorize("hasAuthority('CURRENCIES_WRITE') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Marcar como Divisa Base Principal", description = "Establece la divisa especificada como divisa base principal de la organización y desmarca la anterior.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Divisa base establecida con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict de estado de la divisa")
    })
    public ResponseEntity<ApiResponse<CurrencyResponse>> setBaseCurrency(@PathVariable UUID id) {
        CurrencyResponse response = currencyUseCase.setBaseCurrency(id);
        return ResponseEntity.ok(ApiResponse.ok("Divisa base establecida con éxito", response));
    }
}
