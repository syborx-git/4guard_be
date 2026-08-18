package com.fourguard.wms.domain.enums;

/**
 * License validity status for a Forklift Operator DC-3 certification.
 * Computed dynamically from the expiration date:
 * <ul>
 *   <li>{@link #VIGENTE}    — More than 30 days remaining.</li>
 *   <li>{@link #POR_VENCER} — Between 0 and 30 days remaining (alert window).</li>
 *   <li>{@link #VENCIDA}    — Expiration date is in the past.</li>
 * </ul>
 */
public enum LicenseStatus {
    VIGENTE,
    POR_VENCER,
    VENCIDA
}
