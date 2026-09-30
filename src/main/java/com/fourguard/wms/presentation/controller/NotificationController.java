package com.fourguard.wms.presentation.controller;

import com.fourguard.wms.application.dto.response.notification.NotificationResponse;
import com.fourguard.wms.domain.model.quality.QualityAlertEvent;
import com.fourguard.wms.domain.ports.in.NotificationUseCase;
import com.fourguard.wms.infrastructure.notification.QualityAlertBroadcaster;
import com.fourguard.wms.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for in-app notifications and real-time RF Terminal streaming.
 */
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notificaciones", description = "Gestión de notificaciones in-app y canal de eventos SSE para Terminales RF")
public class NotificationController {

    private final NotificationUseCase notificationUseCase;
    private final QualityAlertBroadcaster alertBroadcaster;

    @GetMapping
    @Operation(
            summary = "Listar mis notificaciones",
            description = "Devuelve las notificaciones del usuario autenticado. " +
                          "Filtrar solo no leídas con ?unreadOnly=true")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Notificaciones recuperadas con éxito"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autenticado")
    })
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getMyNotifications(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            Principal principal) {

        List<NotificationResponse> notifications =
                notificationUseCase.getMyNotifications(principal.getName(), unreadOnly);

        return ResponseEntity.ok(
                ApiResponse.ok("Notificaciones recuperadas con éxito", notifications));
    }

    @PatchMapping("/{id}/read")
    @Operation(
            summary = "Marcar notificación como leída",
            description = "Marca una notificación específica como leída. Solo el destinatario puede realizar esta acción.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Notificación marcada como leída"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autenticado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Notificación no encontrada o no pertenece al usuario")
    })
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable UUID id,
            Principal principal) {

        notificationUseCase.markAsRead(id, principal.getName());
        return ResponseEntity.ok(ApiResponse.ok("Notificación marcada como leída"));
    }

    @GetMapping(value = "/rf-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Canal SSE de Alertas en Tiempo Real para Terminal RF",
               description = "Establece conexión Server-Sent Events (SSE) persistente para recibir alertas de bloqueos QM, acondicionamiento y contingencias ambientales.")
    public SseEmitter subscribeRfStream(
            @RequestParam(defaultValue = "RF-TERMINAL-GENERIC") String terminalId) {

        return alertBroadcaster.subscribe(terminalId);
    }

    @GetMapping("/rf-alerts/recent")
    @Operation(summary = "Obtener alertas recientes para Terminal RF",
               description = "Retorna el buffer de alertas prioritarias activas para sincronización offline.")
    public ResponseEntity<ApiResponse<List<QualityAlertEvent>>> getRecentRfAlerts() {
        return ResponseEntity.ok(ApiResponse.ok("Alertas recientes obtenidas", alertBroadcaster.getRecentAlerts()));
    }
}

