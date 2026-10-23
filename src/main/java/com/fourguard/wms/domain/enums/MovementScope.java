package com.fourguard.wms.domain.enums;

/**
 * Alcance operativo de los listados de movimientos.
 */
public enum MovementScope {
    /** Sin filtro adicional (comportamiento histórico). */
    ALL,
    /** Estados activos, incluyendo arrastre de sesiones anteriores. */
    CURRENT,
    /** Estados cerrados/cancelados. */
    HISTORY;

    public static MovementScope from(String value) {
        if (value == null || value.isBlank()) return ALL;
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return ALL;
        }
    }
}
