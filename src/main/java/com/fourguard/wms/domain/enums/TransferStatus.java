package com.fourguard.wms.domain.enums;

/**
 * Lifecycle status of a warehouse transfer (Cambio de Almacén).
 * Stored as VARCHAR(20) in wms.warehouse_transfers.status.
 * Homologado con ADR-011, ADR-017, ADR-020 y ADR-022.
 */
public enum TransferStatus {
    DRAFT,
    CONFIRMED,
    ASSIGNED,
    IN_PROGRESS,
    PAUSED,
    COMPLETED,
    CANCELLED
}
