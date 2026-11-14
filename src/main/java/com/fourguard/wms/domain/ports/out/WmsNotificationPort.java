package com.fourguard.wms.domain.ports.out;

import com.fourguard.wms.application.dto.websocket.ForkliftMissionEventDto;
import com.fourguard.wms.application.dto.websocket.GateEventDto;
import com.fourguard.wms.application.dto.websocket.QualityBlockEventDto;
import com.fourguard.wms.application.dto.websocket.RampEventDto;

import java.util.UUID;

/**
 * Puerto de Dominio (Outbound Port) para la emisión reactiva de eventos operativos en tiempo real.
 * Desacopla la lógica de negocio y casos de uso del protocolo físico STOMP/WebSocket.
 */
public interface WmsNotificationPort {

    /**
     * Emite evento de caseta (Pre-registro QR, Check-in autorizado, Check-out).
     */
    void broadcastGateEvent(UUID organizationId, UUID branchId, GateEventDto event);

    /**
     * Emite evento de Mesa de Control / Andenes (Rampa asignada, liberada, bloqueada).
     */
    void broadcastRampEvent(UUID organizationId, UUID branchId, RampEventDto event);

    /**
     * Emite misión operativa dirigida al pool de montacarguistas de la sucursal.
     */
    void broadcastMissionEvent(UUID organizationId, UUID branchId, ForkliftMissionEventDto event);

    /**
     * Emite misión operativa dirigida a la cola privada de un montacarguista específico.
     */
    void sendPersonalMissionEvent(String username, ForkliftMissionEventDto event);

    /**
     * Emite alerta de retención o liberación de calidad a la sucursal y terminales RF.
     */
    void broadcastQualityAlert(UUID organizationId, UUID branchId, QualityBlockEventDto event);
}
