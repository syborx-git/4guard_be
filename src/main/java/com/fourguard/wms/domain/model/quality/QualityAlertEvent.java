package com.fourguard.wms.domain.model.quality;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Evento de Alerta de Calidad e Inocuidad transmitido en tiempo real a Terminales RF y Consola.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QualityAlertEvent {

    private UUID eventId;
    private String eventType; // QM_BLOCK_ALERT, QM_CONDITIONING_REQUIRED, QM_RELEASE_AUTHORIZED, ENVIRONMENTAL_ALERT
    private String severity; // CRITICAL (RED), WARNING (YELLOW), INFO (BLUE)
    private String sscc;
    private String palletFolio; // Ej. BLQ-2026-0012 o LIB-2026-0081
    private String sku;
    private String productName;
    private String locationCode;
    private String reason;
    private String recommendedAction;
    private String requiredInstruction; // IT01-PO-GC-8.6-01, IT02-PO-GC-8.6-02, etc.
    private Map<String, Object> metadata;
    private OffsetDateTime timestamp;
}
