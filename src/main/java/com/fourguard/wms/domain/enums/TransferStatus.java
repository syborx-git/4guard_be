package com.fourguard.wms.domain.enums;

/**
 * Lifecycle status of a warehouse transfer (Cambio de Almacén).
 * Stored as VARCHAR(20) in wms.warehouse_transfers.status.
 */
public enum TransferStatus {
    DRAFT,
    CONFIRMED,
    COMPLETED,
    CANCELLED
}
