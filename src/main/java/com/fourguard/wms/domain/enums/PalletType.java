package com.fourguard.wms.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Type of pallet used in warehouse operations (7 tipos oficiales según catálogo maestro del cliente).
 * Stored as VARCHAR(50) in warehouse_receptions and warehouse_reception_pallets.
 */
@Getter
@RequiredArgsConstructor
public enum PalletType {
    MADERA_OWENS("MADERA OWENS", "Madera Owens"),
    MADERA_ESTANDAR("MADERA ESTANDAR", "Madera Estándar"),
    PLASTICO_NEGRO_OWENS("PLASTICO NEGRO OWENS", "Plástico Negro Owens"),
    PLASTICO_AZUL("PLASTICO AZUL", "Plástico Azul"),
    TARIMA_CHEP_NACIONAL("TARIMA CHEP NACIONAL", "Tarima CHEP Nacional"),
    TARIMA_CHEP_EXPORTACION("TARIMA CHEP EXPORTACION", "Tarima CHEP Exportación"),
    TARIMA_PLASTICO_NEGRO_ESTANDAR("TARIMA PLASTICO NEGRO ESTANDAR", "Tarima Plástico Negro Estándar");

    private final String code;
    private final String description;
}
