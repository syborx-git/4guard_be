package com.fourguard.wms.shared.audit;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

/**
 * Helper centralizado para obtener el usuario autenticado del SecurityContext.
 *
 * <p>El JwtAuthenticationFilter ya inyecta el Authentication en el contexto
 * en cada request, por lo que siempre está disponible en cualquier Service.</p>
 */
@Component
public class SecurityAuditHelper {

    private static final String FALLBACK_USER = "SYSTEM";
    private static final Set<String> DIRECTOR_GENERAL_ROLES = Set.of("ADMIN", "CEO", "SUPER_ADMIN");
    private static final Set<String> OPERATIONS_MANAGER_ROLES = Set.of("OPERATIONS_MANAGER", "WAREHOUSE_MANAGER");

    /**
     * Retorna el username del usuario actualmente autenticado.
     * Si por algún motivo no hay autenticación activa (e.g. jobs internos), retorna "SYSTEM".
     *
     * @return username del usuario logueado, o "SYSTEM" como fallback
     */
    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName())) {
            return authentication.getName();
        }
        return FALLBACK_USER;
    }

    /**
     * Determina si el nombre de rol corresponde a un Director General (ADMIN, CEO, SUPER_ADMIN).
     */
    public boolean isDirectorGeneralRole(String roleName) {
        if (roleName == null) {
            return false;
        }
        return DIRECTOR_GENERAL_ROLES.contains(normalizeRoleName(roleName));
    }

    /**
     * Determina si el nombre de rol corresponde a un Gerente de Operaciones (OPERATIONS_MANAGER, WAREHOUSE_MANAGER).
     */
    public boolean isOperationsManagerRole(String roleName) {
        if (roleName == null) {
            return false;
        }
        return OPERATIONS_MANAGER_ROLES.contains(normalizeRoleName(roleName));
    }

    /**
     * Indica si el usuario actualmente autenticado tiene rol de Director General.
     */
    public boolean isCurrentUserDirectorGeneral() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getAuthorities() == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(this::isDirectorGeneralRole);
    }

    /**
     * Indica si el usuario actualmente autenticado es Gerente de Operaciones y NO es Director General.
     */
    public boolean isCurrentUserOperationsManager() {
        if (isCurrentUserDirectorGeneral()) {
            return false;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getAuthorities() == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(this::normalizeRoleName)
                .anyMatch(OPERATIONS_MANAGER_ROLES::contains);
    }

    private String normalizeRoleName(String roleName) {
        if (roleName == null) {
            return "";
        }
        String normalized = roleName.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring(5);
        }
        return normalized;
    }
}
