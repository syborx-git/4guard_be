package com.fourguard.wms.infrastructure.security.websocket;

import com.fourguard.wms.domain.ports.out.TokenBlacklistPort;
import com.fourguard.wms.domain.ports.out.UserRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.UserEntity;
import com.fourguard.wms.infrastructure.persistence.repository.chat.ChatParticipantRepository;
import com.fourguard.wms.infrastructure.security.jwt.JwtService;
import com.fourguard.wms.shared.constants.SecurityConstants;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Interceptor de seguridad para frames STOMP entrantes.
 *
 * <p>Responsabilidades:
 * 1. Frame CONNECT: Valida JWT Bearer, verifica lista negra, inyecta {@link WmsUserPrincipal}.
 * 2. Frame SUBSCRIBE: Valida aislamiento estricto multi-tenancy (organizationId, branchId).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
@RequiredArgsConstructor
@Slf4j
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final TokenBlacklistPort tokenBlacklistPort;
    private final UserRepositoryPort userRepositoryPort;
    private final ChatParticipantRepository chatParticipantRepository;

    // Patrón canónico para tópicos multi-tenant:
    // /topic/org/{orgId}/branch/{branchId}/{module}
    // /topic/org/{orgId}/{module}
    // /topic/chat.{conversationId}
    private static final Pattern TOPIC_BRANCH_PATTERN =
            Pattern.compile("^/topic/org/([0-9a-fA-F\\-]+)/branch/([0-9a-fA-F\\-]+)/([a-zA-Z0-9_\\-]+)$");
    private static final Pattern TOPIC_ORG_PATTERN =
            Pattern.compile("^/topic/org/([0-9a-fA-F\\-]+)/([a-zA-Z0-9_\\-]+)$");
    private static final Pattern TOPIC_CHAT_PATTERN =
            Pattern.compile("^/topic/chat\\.([0-9a-fA-F\\-]+)$");

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();
        if (StompCommand.CONNECT.equals(command)) {
            handleConnect(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(command)) {
            handleSubscribe(accessor);
        } else if (StompCommand.SEND.equals(command)) {
            String dest = accessor.getDestination();
            if (dest != null && (dest.startsWith("/topic/") || dest.startsWith("/queue/"))) {
                throw new AccessDeniedException("Envío directo a tópicos o colas STOMP no permitido");
            }
        }

        return message;
    }

    private void handleConnect(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader(SecurityConstants.AUTHORIZATION_HEADER);
        if (authHeader == null || authHeader.isBlank()) {
            authHeader = accessor.getFirstNativeHeader("authorization");
        }

        if (authHeader == null || !authHeader.startsWith(SecurityConstants.BEARER_PREFIX)) {
            log.warn("[STOMP-AUTH] Frame CONNECT rechazado: Encabezado 'Authorization: Bearer <token>' ausente");
            throw new AuthenticationCredentialsNotFoundException("Token JWT ausente en encabezados de conexión STOMP");
        }

        String token = authHeader.substring(SecurityConstants.BEARER_PREFIX.length()).trim();

        try {
            String username = jwtService.extractUsername(token);
            UUID userId = jwtService.extractUserId(token);
            Date issuedAt = jwtService.extractIssuedAt(token);

            if (userId != null && tokenBlacklistPort.isUserRevoked(userId, issuedAt)) {
                log.warn("[STOMP-AUTH] Conexión rechazada: Sesión revocada para usuario '{}'", username);
                throw new BadCredentialsException("Sesión de usuario revocada en lista negra");
            }

            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            if (!jwtService.isTokenValid(token, userDetails.getUsername())) {
                log.warn("[STOMP-AUTH] Token expirado o inválido para usuario '{}'", username);
                throw new BadCredentialsException("Token JWT expirado o inválido");
            }

            // Obtener datos de Tenancy (organizationId y branchId)
            UserEntity userEntity = userRepositoryPort.findByUsernameOrEmail(username)
                    .orElseThrow(() -> new BadCredentialsException("Usuario no encontrado en base de datos"));

            UUID orgId = userEntity.getOrganization() != null ? userEntity.getOrganization().getId() : null;
            UUID branchId = userEntity.getBranch() != null ? userEntity.getBranch().getId() : null;
            String roleName = userEntity.getRole() != null ? userEntity.getRole().getName() : "OPERATOR";

            List<SimpleGrantedAuthority> authorities = userEntity.getRole() != null
                    ? userEntity.getRole().getPermissions().stream()
                        .map(p -> new SimpleGrantedAuthority(p.getName()))
                        .collect(Collectors.toList())
                    : new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName));

            WmsUserPrincipal principal = new WmsUserPrincipal(
                    userId,
                    username,
                    orgId,
                    branchId,
                    roleName,
                    authorities
            );

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, authorities);

            // Inyectar Principal en el STOMP Header Accessor
            accessor.setUser(authentication);

            // Inyectar en atributos de sesión STOMP para rápido acceso en SUBSCRIBE
            Map<String, Object> sessionAttrs = accessor.getSessionAttributes();
            if (sessionAttrs != null) {
                sessionAttrs.put("userPrincipal", principal);
                sessionAttrs.put("organizationId", orgId);
                sessionAttrs.put("branchId", branchId);
            }

            log.info("[STOMP-AUTH] Conexión autorizada - Usuario: '{}', Org: '{}', Sucursal: '{}', Rol: '{}'",
                    username, orgId, branchId, roleName);

        } catch (ExpiredJwtException e) {
            log.warn("[STOMP-AUTH] Token JWT expirado: {}", e.getMessage());
            throw new BadCredentialsException("Token JWT expirado");
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("[STOMP-AUTH] Fallo de firma o parseo JWT: {}", e.getMessage());
            throw new BadCredentialsException("Token JWT inválido: " + e.getMessage());
        }
    }

    private void handleSubscribe(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null || destination.isBlank()) {
            return;
        }

        var auth = accessor.getUser();
        if (auth == null || !(auth instanceof UsernamePasswordAuthenticationToken authToken)) {
            throw new AccessDeniedException("No autenticado para suscribirse a destinos STOMP");
        }

        Object principalObj = authToken.getPrincipal();
        if (!(principalObj instanceof WmsUserPrincipal principal)) {
            throw new AccessDeniedException("Principal inválido en sesión STOMP");
        }

        if (destination.startsWith("/user/")) {
            // Spring MessageBroker enruta automáticamente colas privadas /user/{name}/...
            return;
        }

        // Validación 0: Tópicos de Chat Empresarial /topic/chat.{conversationId}
        Matcher chatMatcher = TOPIC_CHAT_PATTERN.matcher(destination);
        if (chatMatcher.matches()) {
            UUID conversationId = UUID.fromString(chatMatcher.group(1));
            if (principal.userId() == null || !chatParticipantRepository.existsByConversationIdAndUserIdAndIsActiveTrue(conversationId, principal.userId())) {
                log.warn("[STOMP-CHAT] VIOLACIÓN: Usuario '{}' intentó suscribirse a conversación {} sin ser participante activo",
                        principal.username(), conversationId);
                throw new AccessDeniedException("Acceso denegado: No eres participante activo de esta conversación");
            }
            return;
        }

        // Validación 1: Tópicos con jerarquía /topic/org/{orgId}/branch/{branchId}/...
        Matcher branchMatcher = TOPIC_BRANCH_PATTERN.matcher(destination);
        if (branchMatcher.matches()) {
            String topicOrgStr = branchMatcher.group(1);
            String topicBranchStr = branchMatcher.group(2);
            String module = branchMatcher.group(3);

            UUID topicOrgId = UUID.fromString(topicOrgStr);
            UUID topicBranchId = UUID.fromString(topicBranchStr);

            // Aislamiento Multi-Tenancy Organización
            if (principal.organizationId() == null || !principal.organizationId().equals(topicOrgId)) {
                log.error("[STOMP-TENANCY] VIOLACIÓN: Usuario '{}' (Org: {}) intentó suscribirse a tópico de Org: {}",
                        principal.username(), principal.organizationId(), topicOrgId);
                throw new AccessDeniedException("Acceso denegado: Violación de aislamiento por organización");
            }

            // Aislamiento Multi-Tenancy Sucursal (Salvo Administrador Global / SUPER_ADMIN)
            boolean isGlobalAdmin = "SUPER_ADMIN".equalsIgnoreCase(principal.role()) || "ADMIN".equalsIgnoreCase(principal.role());
            if (!isGlobalAdmin && principal.branchId() != null && !principal.branchId().equals(topicBranchId)) {
                log.error("[STOMP-TENANCY] VIOLACIÓN: Usuario '{}' (Sucursal: {}) intentó suscribirse a Sucursal: {}",
                        principal.username(), principal.branchId(), topicBranchId);
                throw new AccessDeniedException("Acceso denegado: Violación de aislamiento por sucursal");
            }

            log.debug("[STOMP-TENANCY] Suscripción aprobada: Usuario '{}' a módulo '{}' en Org/Sucursal {}/{}",
                    principal.username(), module, topicOrgId, topicBranchId);
            return;
        }

        // Validación 2: Tópicos de nivel Organización /topic/org/{orgId}/...
        Matcher orgMatcher = TOPIC_ORG_PATTERN.matcher(destination);
        if (orgMatcher.matches()) {
            String topicOrgStr = orgMatcher.group(1);
            UUID topicOrgId = UUID.fromString(topicOrgStr);

            if (principal.organizationId() == null || !principal.organizationId().equals(topicOrgId)) {
                log.error("[STOMP-TENANCY] VIOLACIÓN: Usuario '{}' (Org: {}) intentó suscribirse a tópico Org: {}",
                        principal.username(), principal.organizationId(), topicOrgId);
                throw new AccessDeniedException("Acceso denegado: Violación de aislamiento por organización");
            }
            return;
        }

        // Destinos públicos o sin restricción explícita
        if (destination.startsWith("/topic/public")) {
            return;
        }

        log.warn("[STOMP-TENANCY] Intento de suscripción a destino desconocido o no permitido: {}", destination);
        throw new AccessDeniedException("Destino no permitido: " + destination);
    }
}
