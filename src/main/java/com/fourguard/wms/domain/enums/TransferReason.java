package com.fourguard.wms.domain.enums;

/**
 * Reason codes for internal warehouse transfers (Cambio de Almacén).
 * Stored as VARCHAR(30) in wms.warehouse_transfers.reason_code.
 * Mirrors the TRANSFER_REASONS constant array in the Angular frontend model.
 */
public enum TransferReason {
    /** Reorganización de ubicaciones para mejorar el aprovechamiento volumétrico. */
    OPT_ESPACIO,

    /** Movimiento preventivo o preparativo para surtido y despacho. */
    REUB_OPERATIVA,

    /** Desocupación de bahía para recepción, auditoría o mantenimiento. */
    LIB_BAHIA,

    /** Agrupación de lotes y UAs compatibles en una sola posición. */
    CONSOLIDACION,

    /** Instrucción directa del cliente para segregar o trasladar mercancía. */
    SOL_CLIENTE,

    /** Aislamiento temporal de tarimas por inspección de calidad QM. */
    INCIDENCIA,

    /** Motivo extraordinario no catalogado. Requires observations. */
    OTRO
}
