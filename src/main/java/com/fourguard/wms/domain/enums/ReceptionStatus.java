package com.fourguard.wms.domain.enums;

/**
 * Lifecycle status of a warehouse reception (F01).
 * Stored as VARCHAR(20) in wms.warehouse_receptions.status.
 */
public enum ReceptionStatus {

    /** Pre-reception registered at caseta de seguridad (Vigilancia). Pending dock & operator assignment. */
    REGISTERED,

    /** Assigned by warehouse administrative to dock ramp and forklift operator. Ramp is locked. */
    ASSIGNED,

    /** Active unloading in progress by the forklift operator reading codes/UAs at the dock. */
    IN_PROGRESS,

    /** Physical unloading finished by forklift operator; payload returned to administrative for audit. */
    DISCHARGED,

    /** Reception fully completed and authorized by warehouse supervisor. UAs entered inventory. Ramp unlocked. */
    COMPLETED,

    /** Reception cancelled by admin with mandatory justification. Ramp unlocked. */
    CANCELLED
}
