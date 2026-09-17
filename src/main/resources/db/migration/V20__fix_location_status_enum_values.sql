-- =============================================================================
-- V20: Fix Location Status enum values in wms.locations
-- Description: Normalizes legacy 'AVAILABLE' status values to 'ACTIVE' to align
--              with com.fourguard.wms.domain.enums.LocationStatus (ACTIVE, BLOCKED, MAINTENANCE, INACTIVE).
-- =============================================================================

SET search_path TO wms, public;

-- 1. Correct any legacy or misaligned status values in existing databases
UPDATE wms.locations
SET status = 'ACTIVE'
WHERE status = 'AVAILABLE' OR status IS NULL;

-- 2. Ensure constraint matches LocationStatus Java enum exactly
ALTER TABLE wms.locations 
    DROP CONSTRAINT IF EXISTS chk_locations_status;

ALTER TABLE wms.locations 
    ADD CONSTRAINT chk_locations_status 
    CHECK (status IN ('ACTIVE', 'BLOCKED', 'MAINTENANCE', 'INACTIVE'));
