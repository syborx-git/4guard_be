package com.fourguard.wms.domain.enums;

/**
 * Functional category of a warehouse storage location.
 * - FIXED_STORAGE: Standard nominal warehouse capacity (100% baseline).
 * - TEMPORARY_BUFFER: Temporary staging/holding location representing overflow / extra percentage.
 * - PRELOAD_STAGING: Preload / staging ramp locations for dispatch or preparation.
 */
public enum LocationCategory {
    FIXED_STORAGE,
    TEMPORARY_BUFFER,
    PRELOAD_STAGING
}
