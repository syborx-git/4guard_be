package com.fourguard.wms.infrastructure.scheduler;

import com.fourguard.wms.domain.ports.in.ExchangeRateUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateScheduler {

    private final ExchangeRateUseCase exchangeRateUseCase;

    /**
     * Sincronización automática de tipos de cambio con Banxico SIE.
     * Se ejecuta de Lunes a Viernes a las 9:00 AM (Zona America/Mexico_City).
     */
    @Scheduled(cron = "0 0 9 * * MON-FRI", zone = "America/Mexico_City")
    public void syncBanxicoRatesJob() {
        log.info("[JOB-BANXICO] Iniciando Job programado de sincronización de tipos de cambio con Banxico...");
        try {
            // Organización por defecto: 4GUARD LOGISTICS CORP
            UUID defaultOrgId = UUID.fromString("a53f0907-9fa5-4bdf-87db-2eb5e7683935");
            exchangeRateUseCase.syncBanxicoRates(defaultOrgId);
            log.info("[JOB-BANXICO] Job de sincronización con Banxico completado con éxito.");
        } catch (Exception e) {
            log.error("[JOB-BANXICO-ERROR] Error en el Job de sincronización con Banxico: {}", e.getMessage(), e);
        }
    }
}
