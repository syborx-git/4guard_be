package com.fourguard.wms.infrastructure.notification;

import com.fourguard.wms.domain.model.quality.QualityAlertEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Broadcaster reactivo de eventos y alertas hacia las Terminales RF (PWA) de Montacarguistas
 * y la Consola de Calidad mediante Server-Sent Events (SSE).
 */
@Component
@Slf4j
public class QualityAlertBroadcaster {

    private final List<SseEmitter> activeEmitters = new CopyOnWriteArrayList<>();
    private final List<QualityAlertEvent> recentAlerts = new CopyOnWriteArrayList<>();
    private static final int MAX_RECENT_ALERTS = 25;

    public SseEmitter subscribe(String clientIdentifier) {
        // Timeout de 30 minutos por conexión SSE
        SseEmitter emitter = new SseEmitter(1800000L);

        this.activeEmitters.add(emitter);
        log.info("[QualityAlertBroadcaster] Terminal RF conectada ({}) - Clientes activos: {}", clientIdentifier, activeEmitters.size());

        emitter.onCompletion(() -> {
            activeEmitters.remove(emitter);
            log.debug("[QualityAlertBroadcaster] Conexión SSE finalizada para {}", clientIdentifier);
        });

        emitter.onTimeout(() -> {
            activeEmitters.remove(emitter);
            log.debug("[QualityAlertBroadcaster] Conexión SSE expiró por timeout para {}", clientIdentifier);
        });

        emitter.onError(e -> {
            activeEmitters.remove(emitter);
            log.debug("[QualityAlertBroadcaster] Error en conexión SSE de {}: {}", clientIdentifier, e.getMessage());
        });

        // Enviar evento de bienvenida
        try {
            emitter.send(SseEmitter.event()
                    .name("INIT_CONNECTION")
                    .data("{\"status\":\"CONNECTED\",\"message\":\"Canal de Alertas RF 4GUARD Activo\"}"));
        } catch (IOException e) {
            activeEmitters.remove(emitter);
        }

        return emitter;
    }

    public void broadcast(QualityAlertEvent event) {
        log.info("[QualityAlertBroadcaster] Emitiendo alerta '{}' para SSCC '{}' (Severidad: {})",
                event.getEventType(), event.getSscc(), event.getSeverity());

        // Guardar en buffer circular de alertas recientes
        recentAlerts.add(0, event);
        if (recentAlerts.size() > MAX_RECENT_ALERTS) {
            recentAlerts.remove(recentAlerts.size() - 1);
        }

        List<SseEmitter> deadEmitters = new ArrayList<>();

        for (SseEmitter emitter : activeEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(event.getEventType())
                        .data(event));
            } catch (Exception e) {
                deadEmitters.add(emitter);
            }
        }

        activeEmitters.removeAll(deadEmitters);
    }

    public List<QualityAlertEvent> getRecentAlerts() {
        return new ArrayList<>(recentAlerts);
    }
}
