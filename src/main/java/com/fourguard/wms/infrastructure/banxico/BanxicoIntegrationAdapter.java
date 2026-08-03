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
            log.warn("[BANXICO] Sincronización omitida: Token de Banxico no configurado en el archivo .env");
            return Optional.empty();
        }

        try {
            LocalDate endDateObj = LocalDate.now();
            LocalDate startDateObj = endDateObj.minusDays(5);
            String endDate = endDateObj.format(DateTimeFormatter.ISO_LOCAL_DATE);
            String startDate = startDateObj.format(DateTimeFormatter.ISO_LOCAL_DATE);

            String fullUrl = String.format("%s/%s/datos/%s/%s?token=%s",
                    banxicoUrl, series.getSeriesId(), startDate, endDate, banxicoToken);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Bmx-Token", banxicoToken);
            headers.set("Accept", "application/json");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            log.info("[BANXICO] Consultando tipo de cambio para serie {} ({}) con rango 5 días ({} a {})...",
                    series.getSeriesId(), series.getCurrencyCode(), startDate, endDate);

            ResponseEntity<String> response = restTemplate.exchange(fullUrl, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseLatestRate(response.getBody(), series);
            }
        } catch (Exception e) {
            log.error("[BANXICO-ERROR] Error al consultar la API de Banxico para serie {}: {}", series.getSeriesId(), e.getMessage());
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
            log.warn("[BANXICO] Sincronización omitida: Token de Banxico no configurado en el archivo .env");
            return Optional.empty();
        }

        try {
            LocalDate endDateObj = LocalDate.now();
            LocalDate startDateObj = endDateObj.minusDays(5);
            String endDate = endDateObj.format(DateTimeFormatter.ISO_LOCAL_DATE);
            String startDate = startDateObj.format(DateTimeFormatter.ISO_LOCAL_DATE);

            String fullUrl = String.format("%s/%s/datos/%s/%s?token=%s",
                    banxicoUrl, seriesId.trim(), startDate, endDate, banxicoToken);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Bmx-Token", banxicoToken);
            headers.set("Accept", "application/json");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            log.info("[BANXICO] Consulta en tiempo real para serieId {} con rango 5 días ({} a {})...",
                    seriesId, startDate, endDate);

            ResponseEntity<String> response = restTemplate.exchange(fullUrl, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
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
                            String currencyCode = resolveCurrencyCode(seriesId);

                            BanxicoLiveRateResponse liveResponse = BanxicoLiveRateResponse.builder()
                                    .seriesId(seriesId)
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
            }
        } catch (Exception e) {
            log.error("[BANXICO-ERROR] Error al consultar en tiempo real serie {}: {}", seriesId, e.getMessage());
        }
        return Optional.empty();
    }

    private String resolveCurrencyCode(String seriesId) {
        for (BanxicoSeries s : BanxicoSeries.values()) {
            if (s.getSeriesId().equalsIgnoreCase(seriesId)) {
                return s.getCurrencyCode();
            }
        }
        return "UNKNOWN";
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
