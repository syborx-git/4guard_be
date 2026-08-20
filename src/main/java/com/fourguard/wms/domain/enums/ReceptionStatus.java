package com.fourguard.wms.domain.enums;

/**
 * Lifecycle status of a warehouse reception (F01).
 * Stored as VARCHAR(20) in wms.warehouse_receptions.status.
 */
public enum ReceptionStatus {

    /** Pre-reception registered at caseta de seguridad. Pending unloading. */
    REGISTERED,

    /** Reception fully completed and authorized by warehouse leader. UAs entered inventory. */
    COMPLETED,

    /** Reception cancelled by admin with mandatory justification. */
    CANCELLED
}
