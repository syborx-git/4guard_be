package com.fourguard.wms.infrastructure.security.websocket;

import org.springframework.security.core.GrantedAuthority;

import java.security.Principal;
import java.util.Collection;
import java.util.UUID;

/**
 * Representación inmutable del usuario autenticado en la sesión STOMP.
 * Almacena identificadores de Tenancy (Organización y Sucursal) para validación rápida en SUBSCRIBE.
 */
public record WmsUserPrincipal(
        UUID userId,
        String username,
        UUID organizationId,
        UUID branchId,
        String role,
        Collection<? extends GrantedAuthority> authorities
) implements Principal {
    @Override
    public String getName() {
        return username;
    }
}
