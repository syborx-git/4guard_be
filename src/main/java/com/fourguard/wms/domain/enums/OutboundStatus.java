package com.fourguard.wms.domain.enums;

/**
 * Lifecycle status of a warehouse outbound dispatch (Salida F03).
 * Stored as VARCHAR(20) in wms.warehouse_outbounds.status.
 */
public enum OutboundStatus {
    DRAFT,
    CONFIRMED,
    COMPLETED,
    CANCELLED
}
