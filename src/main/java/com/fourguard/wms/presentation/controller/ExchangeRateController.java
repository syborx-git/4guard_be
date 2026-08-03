package com.fourguard.wms.presentation.controller;

import com.fourguard.wms.application.dto.request.ConvertCurrencyRequest;
import com.fourguard.wms.application.dto.request.CreateExchangeRateRequest;
import com.fourguard.wms.application.dto.response.BanxicoLiveRateResponse;
import com.fourguard.wms.application.dto.response.ConvertCurrencyResponse;
import com.fourguard.wms.application.dto.response.ExchangeRateResponse;
import com.fourguard.wms.application.dto.response.ParityMatrixResponse;
import com.fourguard.wms.domain.ports.in.ExchangeRateUseCase;
import com.fourguard.wms.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * REST Controller para Gestión e Histórico de Tipos de Cambio y Paridades.
 * Base path: /exchange-rates
 */
@RestController
@RequestMapping("/exchange-rates")
@RequiredArgsConstructor
@Tag(name = "Tipos de Cambio", description = "Endpoints para registro, consulta, matriz de paridades y calculadora de conversión de divisas")
public class ExchangeRateController {

    private final ExchangeRateUseCase exchangeRateUseCase;

    @GetMapping
    @PreAuthorize("hasAuthority('EXCHANGE_RATES_READ') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Listar tipos de cambio", description = "Obtiene el historial de tipos de cambio filtrable por organización, paridades de divisas y fecha de efectividad.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lista de tipos de cambio recuperada con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado")
    })
    public ResponseEntity<ApiResponse<List<ExchangeRateResponse>>> getExchangeRates(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) String fromCode,
            @RequestParam(required = false) String toCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<ExchangeRateResponse> response = exchangeRateUseCase.getExchangeRates(organizationId, fromCode, toCode, date);
        return ResponseEntity.ok(ApiResponse.ok("Lista de tipos de cambio recuperada con éxito", response));
    }

    @GetMapping("/latest")
    @PreAuthorize("hasAuthority('EXCHANGE_RATES_READ') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Matriz de paridades vigentes", description = "Devuelve la matriz de tipos de cambio activos respecto a la divisa base de la organización.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Matriz de paridades devuelta con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado")
    })
    public ResponseEntity<ApiResponse<ParityMatrixResponse>> getLatestParityMatrix(@RequestParam(required = false) UUID organizationId) {
        ParityMatrixResponse response = exchangeRateUseCase.getLatestParityMatrix(organizationId);
        return ResponseEntity.ok(ApiResponse.ok("Matriz de paridades devuelta con éxito", response));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('EXCHANGE_RATES_WRITE') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Registrar tipo de cambio", description = "Registra una tasa de cambio entre dos divisas. Calcula automáticamente la tasa inversa (1 / rate).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tipo de cambio registrado con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Divisa no encontrada")
    })
    public ResponseEntity<ApiResponse<ExchangeRateResponse>> saveExchangeRate(@Valid @RequestBody CreateExchangeRateRequest request) {
        ExchangeRateResponse response = exchangeRateUseCase.saveExchangeRate(request);
        return ResponseEntity.ok(ApiResponse.ok("Tipo de cambio registrado con éxito", response));
    }

    @PostMapping("/convert")
    @PreAuthorize("hasAuthority('EXCHANGE_RATES_READ') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Calculadora de conversión en tiempo real", description = "Convierte un monto entre dos divisas utilizando la tasa vigente (directa, inversa o mediante triangulación por divisa base).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Conversión calculada con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Tasa de cambio no disponible o parámetros inválidos")
    })
    public ResponseEntity<ApiResponse<ConvertCurrencyResponse>> convert(@Valid @RequestBody ConvertCurrencyRequest request) {
        ConvertCurrencyResponse response = exchangeRateUseCase.convert(request);
        return ResponseEntity.ok(ApiResponse.ok("Conversión calculada con éxito", response));
    }

    @PostMapping("/sync/banxico")
    @PreAuthorize("hasAuthority('EXCHANGE_RATES_WRITE') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Forzar sincronización con Banxico SIE", description = "Sincroniza en tiempo real las tasas oficiales de USD (SF57805) y EUR (SF46410) desde la API de Banxico.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Sincronización con Banxico completada con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autorizado")
    })
    public ResponseEntity<ApiResponse<List<ExchangeRateResponse>>> syncBanxicoRates(
            @RequestParam(required = false) UUID organizationId) {
        List<ExchangeRateResponse> response = exchangeRateUseCase.syncBanxicoRates(organizationId);
        return ResponseEntity.ok(ApiResponse.ok("Sincronización con Banxico completada con éxito", response));
    }

    @GetMapping("/banxico/live/{seriesId}")
    @PreAuthorize("hasAuthority('EXCHANGE_RATES_READ') or hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('OPERATIONS_MANAGER')")
    @Operation(summary = "Consultar cotización en tiempo real de Banxico por idSerie",
               description = "Consulta directamente la API SIE de Banxico en tiempo real para obtener la tasa oficial más reciente de una serie (ej. SF57805 para USD, SF46410 para EUR).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cotización recuperada en tiempo real con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Serie inválida o token no configurado")
    })
    public ResponseEntity<ApiResponse<BanxicoLiveRateResponse>> getLiveBanxicoRate(@PathVariable String seriesId) {
        BanxicoLiveRateResponse response = exchangeRateUseCase.fetchLiveBanxicoRateBySeries(seriesId);
        return ResponseEntity.ok(ApiResponse.ok("Cotización oficial recuperada en tiempo real con éxito", response));
    }
}
