package com.fourguard.wms.infrastructure.adapter.notification;

import com.fourguard.wms.application.dto.websocket.*;
import com.fourguard.wms.domain.ports.out.WmsNotificationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Adaptador de Infraestructura que implementa {@link WmsNotificationPort} sobre Spring STOMP.
 * Envía eventos tipados a tópicos jerárquicos multitenant sin bloquear la transacción JPA.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StompNotificationBroadcasterAdapter implements WmsNotificationPort {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void broadcastGateEvent(UUID organizationId, UUID branchId, GateEventDto event) {
        String destination = String.format("/topic/org/%s/branch/%s/gate", organizationId, branchId);
        BaseWsEventDto<GateEventDto> envelope = BaseWsEventDto.of(event.eventType(), organizationId, branchId, event);

        log.info("[WS-STOMP] Difundiendo evento Caseta '{}' en '{}' (Folio: {})",
                event.eventType(), destination, event.folio());
        sendSafe(destination, envelope);
    }

    @Override
    public void broadcastRampEvent(UUID organizationId, UUID branchId, RampEventDto event) {
        String destination = String.format("/topic/org/%s/branch/%s/ramps", organizationId, branchId);
        BaseWsEventDto<RampEventDto> envelope = BaseWsEventDto.of(event.eventType(), organizationId, branchId, event);

        log.info("[WS-STOMP] Difundiendo evento Rampa '{}' en '{}' (Rampa: {}, Folio: {})",
                event.eventType(), destination, event.rampCode(), event.currentFolio());
        sendSafe(destination, envelope);
    }

    @Override
    public void broadcastMissionEvent(UUID organizationId, UUID branchId, ForkliftMissionEventDto event) {
        String destination = String.format("/topic/org/%s/branch/%s/missions", organizationId, branchId);
        BaseWsEventDto<ForkliftMissionEventDto> envelope = BaseWsEventDto.of(event.status(), organizationId, branchId, event);

        log.info("[WS-STOMP] Difundiendo misión montacarguista '{}' en '{}' (Misión: {})",
                event.missionType(), destination, event.folio());
        sendSafe(destination, envelope);
    }

    @Override
    public void sendPersonalMissionEvent(String username, ForkliftMissionEventDto event) {
        String userQueue = "/queue/missions";
        BaseWsEventDto<ForkliftMissionEventDto> envelope = BaseWsEventDto.of(
                event.status(), event.organizationId(), event.branchId(), event);

        log.info("[WS-STOMP] Enviando misión privada a usuario '{}' destino '{}{}' (Misión: {})",
                username, "/user/" + username, userQueue, event.folio());
        try {
            messagingTemplate.convertAndSendToUser(username, userQueue, envelope);
        } catch (Exception e) {
            log.error("[WS-STOMP] Error enviando mensaje privado a '{}': {}", username, e.getMessage());
        }
    }

    @Override
    public void broadcastQualityAlert(UUID organizationId, UUID branchId, QualityBlockEventDto event) {
        String destinationQuality = String.format("/topic/org/%s/branch/%s/quality", organizationId, branchId);
        String destinationMissions = String.format("/topic/org/%s/branch/%s/missions", organizationId, branchId);

        BaseWsEventDto<QualityBlockEventDto> envelope = BaseWsEventDto.of(event.eventType(), organizationId, branchId, event);

        log.warn("[WS-STOMP] DIFUNDIENDO ALERTA CALIDAD '{}' (SSCC: {}, Severidad: {})",
                event.eventType(), event.sscc(), event.severity());

        sendSafe(destinationQuality, envelope);
        // También se replica en el canal de misiones para alertar a montacarguistas activos
        sendSafe(destinationMissions, envelope);
    }

    private void sendSafe(String destination, Object payload) {
        try {
            messagingTemplate.convertAndSend(destination, payload);
        } catch (Exception ex) {
            log.error("[WS-STOMP] Fallo al despachar frame hacia '{}': {}", destination, ex.getMessage());
        }
    }
}
