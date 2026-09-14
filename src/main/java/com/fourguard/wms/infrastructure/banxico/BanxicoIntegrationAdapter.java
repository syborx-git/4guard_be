package com.fourguard.wms.infrastructure.banxico;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fourguard.wms.application.dto.response.BanxicoLiveRateResponse;
import com.fourguard.wms.domain.enums.BanxicoSeries;
import com.fourguard.wms.domain.ports.out.BanxicoExchangeRatePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class BanxicoIntegrationAdapter implements BanxicoExchangeRatePort {

    @Value("${banxico.api.url:https://www.banxico.org.mx/SieAPIRest/service/v1/series}")
    private String banxicoUrl;

    @Value("${banxico.api.token:}")
    private String banxicoToken;

    @Value("${banxico.api.enabled:true}")
    private boolean banxicoEnabled;

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public Optional<BigDecimal> fetchLatestRate(BanxicoSeries series) {
        if (!banxicoEnabled || banxicoToken == null || banxicoToken.isBlank() || banxicoToken.startsWith("PON_TU_TOKEN")) {
            log.warn("[BANXICO] Sincronización omitida: Token de Banxico no configurado o deshabilitado.");
            return Optional.empty();
        }

        String seriesId = series.getSeriesId();
        // 1. Intentar endpoint oficial oportuno (último dato publicado)
        try {
            String oportunoUrl = String.format("%s/%s/datos/oportuno", banxicoUrl.replaceAll("/$", ""), seriesId.trim());
            HttpHeaders headers = new HttpHeaders();
            headers.set("Bmx-Token", banxicoToken.trim());
            headers.set("Accept", "application/json");
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) 4Guard-WMS/1.0");

            HttpEntity<String> entity = new HttpEntity<>(headers);
            log.info("[BANXICO] Consultando tasa oportuna para serie {} ({})...", seriesId, series.getCurrencyCode());

            ResponseEntity<String> response = restTemplate.exchange(oportunoUrl, HttpMethod.GET, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Optional<BigDecimal> rateOpt = parseLatestRate(response.getBody(), series);
                if (rateOpt.isPresent()) {
                    return rateOpt;
                }
            }
        } catch (Exception e) {
            log.warn("[BANXICO-OPORTUNO] Endpoint oportuno falló para {}: {}. Intentando fallback por rango de fechas...", seriesId, e.getMessage());
        }

        // 2. Fallback: Rango de últimos 10 días
        try {
            LocalDate endDateObj = LocalDate.now();
            LocalDate startDateObj = endDateObj.minusDays(10);
            String endDate = endDateObj.format(DateTimeFormatter.ISO_LOCAL_DATE);
            String startDate = startDateObj.format(DateTimeFormatter.ISO_LOCAL_DATE);

            String rangeUrl = String.format("%s/%s/datos/%s/%s", banxicoUrl.replaceAll("/$", ""), seriesId.trim(), startDate, endDate);
            HttpHeaders headers = new HttpHeaders();
            headers.set("Bmx-Token", banxicoToken.trim());
            headers.set("Accept", "application/json");
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) 4Guard-WMS/1.0");

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(rangeUrl, HttpMethod.GET, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseLatestRate(response.getBody(), series);
            }
        } catch (Exception e) {
            log.error("[BANXICO-ERROR] Error en fallback de rango para serie {}: {}", seriesId, e.getMessage());
        }

        return Optional.empty();
    }

    @Override
    public Map<BanxicoSeries, BigDecimal> fetchAllLatestRates() {
        Map<BanxicoSeries, BigDecimal> result = new EnumMap<>(BanxicoSeries.class);
        for (BanxicoSeries series : BanxicoSeries.values()) {
            fetchLatestRate(series).ifPresent(rate -> result.put(series, rate));
        }
        return result;
    }

    @Override
    public Optional<BanxicoLiveRateResponse> fetchLiveRateBySeriesId(String seriesId) {
        if (!banxicoEnabled || banxicoToken == null || banxicoToken.isBlank() || banxicoToken.startsWith("PON_TU_TOKEN")) {
            log.warn("[BANXICO] Consulta Live omitida: Token de Banxico no configurado o deshabilitado.");
            return Optional.empty();
        }

        String cleanSeriesId = (seriesId != null && !seriesId.isBlank()) ? seriesId.trim() : "SF57805";

        // 1. Intentar endpoint oportuno
        try {
            String oportunoUrl = String.format("%s/%s/datos/oportuno", banxicoUrl.replaceAll("/$", ""), cleanSeriesId);
            HttpHeaders headers = new HttpHeaders();
            headers.set("Bmx-Token", banxicoToken.trim());
            headers.set("Accept", "application/json");
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) 4Guard-WMS/1.0");

            HttpEntity<String> entity = new HttpEntity<>(headers);
            log.info("[BANXICO-LIVE] Consultando cotización oportuna en vivo para serieId {}...", cleanSeriesId);

            ResponseEntity<String> response = restTemplate.exchange(oportunoUrl, HttpMethod.GET, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Optional<BanxicoLiveRateResponse> parsed = parseLiveResponse(response.getBody(), cleanSeriesId);
                if (parsed.isPresent()) {
                    return parsed;
                }
            }
        } catch (Exception e) {
            log.warn("[BANXICO-LIVE-OPORTUNO] Consulta oportuna falló para {}: {}. Intentando fallback por rango...", cleanSeriesId, e.getMessage());
        }

        // 2. Fallback: Rango de últimos 10 días
        try {
            LocalDate endDateObj = LocalDate.now();
            LocalDate startDateObj = endDateObj.minusDays(10);
            String endDate = endDateObj.format(DateTimeFormatter.ISO_LOCAL_DATE);
            String startDate = startDateObj.format(DateTimeFormatter.ISO_LOCAL_DATE);

            String rangeUrl = String.format("%s/%s/datos/%s/%s", banxicoUrl.replaceAll("/$", ""), cleanSeriesId, startDate, endDate);
            HttpHeaders headers = new HttpHeaders();
            headers.set("Bmx-Token", banxicoToken.trim());
            headers.set("Accept", "application/json");
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) 4Guard-WMS/1.0");

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(rangeUrl, HttpMethod.GET, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseLiveResponse(response.getBody(), cleanSeriesId);
            }
        } catch (Exception e) {
            log.error("[BANXICO-LIVE-ERROR] Error en fallback live para serie {}: {}", cleanSeriesId, e.getMessage());
        }

        return Optional.empty();
    }

    private Optional<BanxicoLiveRateResponse> parseLiveResponse(String jsonBody, String cleanSeriesId) {
        try {
            JsonNode root = objectMapper.readTree(jsonBody);
            JsonNode seriesNode = root.path("bmx").path("series").path(0);
            String title = seriesNode.path("titulo").asText("Cotización oficial Banxico");
            JsonNode datosArray = seriesNode.path("datos");

            if (datosArray.isArray() && datosArray.size() > 0) {
                for (int i = datosArray.size() - 1; i >= 0; i--) {
                    JsonNode item = datosArray.get(i);
                    String datoStr = item.path("dato").asText(null);
                    String fechaStr = item.path("fecha").asText(null);

                    if (datoStr != null && !datoStr.isBlank() && !"N/E".equalsIgnoreCase(datoStr)) {
                        BigDecimal rate = new BigDecimal(datoStr.trim());
                        String currencyCode = resolveCurrencyCode(cleanSeriesId);

                        BanxicoLiveRateResponse liveResponse = BanxicoLiveRateResponse.builder()
                                .seriesId(cleanSeriesId)
                                .currencyCode(currencyCode)
                                .seriesTitle(title)
                                .rate(rate)
                                .publicationDate(fechaStr)
                                .sourceType("BANXICO_SIE_REST")
                                .build();

                        return Optional.of(liveResponse);
                    }
                }
            }
        } catch (Exception e) {
            log.error("[BANXICO-LIVE-PARSER] Error parseando respuesta live de {}: {}", cleanSeriesId, e.getMessage());
        }
        return Optional.empty();
    }

    private String resolveCurrencyCode(String seriesId) {
        for (BanxicoSeries s : BanxicoSeries.values()) {
            if (s.getSeriesId().equalsIgnoreCase(seriesId)) {
                return s.getCurrencyCode();
            }
        }
        if ("SF57805".equalsIgnoreCase(seriesId)) return "USD";
        if ("SF46410".equalsIgnoreCase(seriesId)) return "EUR";
        return "USD";
    }

    private Optional<BigDecimal> parseLatestRate(String jsonBody, BanxicoSeries series) {
        try {
            JsonNode root = objectMapper.readTree(jsonBody);
            JsonNode datosArray = root.path("bmx").path("series").path(0).path("datos");

            if (datosArray.isArray() && datosArray.size() > 0) {
                for (int i = datosArray.size() - 1; i >= 0; i--) {
                    JsonNode item = datosArray.get(i);
                    String datoStr = item.path("dato").asText(null);
                    String fechaStr = item.path("fecha").asText(null);

                    if (datoStr != null && !datoStr.isBlank() && !"N/E".equalsIgnoreCase(datoStr)) {
                        BigDecimal rate = new BigDecimal(datoStr.trim());
                        log.info("[BANXICO] Tasa recuperada con éxito para {} ({}): {} MXN (Fecha: {})",
                                series.getCurrencyCode(), series.getSeriesId(), rate, fechaStr);
                        return Optional.of(rate);
                    }
                }
            }
        } catch (Exception e) {
            log.error("[BANXICO-PARSER-ERROR] Error al parsear respuesta JSON de Banxico para {}: {}", series.getSeriesId(), e.getMessage());
        }
        return Optional.empty();
    }
}
