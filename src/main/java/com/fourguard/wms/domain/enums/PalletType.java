package com.fourguard.wms.domain.enums;

/**
 * Type of pallet used in warehouse operations.
 * Stored as VARCHAR(30) in warehouse_receptions and warehouse_reception_pallets.
 */
public enum PalletType {
    MADERA_ESTANDAR,
    TARIMA_CHEP,
    PLASTICO,
    PLASTICO_AZUL,
    MADERA_EXPORTACION,
    SIN_TARIMA,
    MADERA
}
