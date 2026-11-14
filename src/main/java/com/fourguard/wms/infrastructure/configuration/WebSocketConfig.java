package com.fourguard.wms.infrastructure.configuration;

import com.fourguard.wms.infrastructure.security.websocket.StompAuthChannelInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

/**
 * Configuración central del Message Broker WebSocket con STOMP nativo para 4GUARD WMS.
 *
 * <p>Diseñada quirúrgicamente para el entorno restringido de Hetzner VPS (< 450MB RAM):
 * <ul>
 *   <li>Scheduler de heartbeats estricto de 1 solo hilo daemon.</li>
 *   <li>Límites estrictos de búferes de transporte para evitar OOM por clientes lentos.</li>
 *   <li>Soporte bimodal: WebSocket nativo y SockJS fallback.</li>
 *   <li>Canal inbound protegido por interceptor JWT con aislamiento multi-tenancy.</li>
 * </ul>
 */
@Configuration
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;
    private final CorsProperties corsProperties;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] allowedOrigins = resolveAllowedOrigins();

        // 1. Endpoint Nativo puro para Clientes STOMP modernos, apps móviles y terminales RF
        registry.addEndpoint("/ws-wms")
                .setAllowedOriginPatterns(allowedOrigins);

        // 2. Endpoint con SockJS fallback para compatibilidad con navegadores restrictivos
        registry.addEndpoint("/ws-wms")
                .setAllowedOriginPatterns(allowedOrigins)
                .withSockJS()
                .setStreamBytesLimit(128 * 1024)
                .setHttpMessageCacheSize(100)
                .setDisconnectDelay(30 * 1000);

        log.info("[WebSocketConfig] STOMP Endpoints registrados en /ws-wms (Nativo + SockJS)");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Habilitar Broker simple en memoria con Heartbeat controlado
        registry.enableSimpleBroker("/topic", "/queue")
                .setTaskScheduler(wsHeartbeatTaskScheduler())
                .setHeartbeatValue(new long[] {15000, 15000}); // Ping/Pong cada 15 segundos

        // Prefijo para mensajes enviados desde el cliente hacia @MessageMapping
        registry.setApplicationDestinationPrefixes("/app");

        // Prefijo para colas privadas específicas de un usuario (/user/queue/...)
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration
                // Límite de tamaño por frame STOMP (64 KB)
                .setMessageSizeLimit(64 * 1024)
                // Límite del búfer de envío por sesión (512 KB) para prevenir buffer-bloat en terminales RF lentas
                .setSendBufferSizeLimit(512 * 1024)
                // Timeout máximo de 15s para vaciar el búfer hacia el socket del cliente
                .setSendTimeLimit(15 * 1000)
                // Timeout para recibir el primer mensaje tras la apertura TCP (20 segundos)
                .setTimeToFirstMessage(20 * 1000);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Registrar interceptor de autenticación y autorización STOMP
        registration.interceptors(stompAuthChannelInterceptor);

        // Pool de ejecución acotado para Hetzner VPS (2 hilos base, máximo 4, cola de 100)
        registration.taskExecutor()
                .corePoolSize(2)
                .maxPoolSize(4)
                .queueCapacity(100)
                .keepAliveSeconds(60);
    }

    /**
     * Scheduler dedicado de 1 solo hilo para heartbeats STOMP.
     * Evita la creación desenfrenada de hilos en JVMs de baja memoria.
     */
    @Bean(name = "wsHeartbeatTaskScheduler")
    public ThreadPoolTaskScheduler wsHeartbeatTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("ws-heartbeat-");
        scheduler.setDaemon(true);
        scheduler.setErrorHandler(t -> log.warn("[WS-Heartbeat] Error en ciclo heartbeat: {}", t.getMessage()));
        scheduler.initialize();
        return scheduler;
    }

    private String[] resolveAllowedOrigins() {
        if (corsProperties != null && corsProperties.getAllowedOriginPatterns() != null
                && !corsProperties.getAllowedOriginPatterns().isEmpty()) {
            return corsProperties.getAllowedOriginPatterns().toArray(new String[0]);
        }
        return new String[] {"*"};
    }
}
