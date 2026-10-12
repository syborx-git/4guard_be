-- ==============================================================================
-- Flyway Migration: V41__standardize_eco_tractor_and_caja_naming.sql
-- Description: Standardizes transport unit economic identifiers according to
--              official F01 Check List standard:
--              - no_eco_tractor (No. Económico Tracto / Cabina)
--              - no_eco_caja (No. Económico Caja / Remolque)
--              Permanently eliminates legacy residual columns:
--              economic_number and box_economic_number.
-- ==============================================================================

SET search_path TO wms, public;

-- 1. Standardize wms.security_pre_checkins
ALTER TABLE wms.security_pre_checkins
    ADD COLUMN IF NOT EXISTS no_eco_caja VARCHAR(100);

UPDATE wms.security_pre_checkins
SET no_eco_tractor = economic_number
WHERE (no_eco_tractor IS NULL OR no_eco_tractor = '') AND economic_number IS NOT NULL AND economic_number <> '';

UPDATE wms.security_pre_checkins
SET no_eco_caja = box_economic_number
WHERE (no_eco_caja IS NULL OR no_eco_caja = '') AND box_economic_number IS NOT NULL AND box_economic_number <> '';

ALTER TABLE wms.security_pre_checkins
    DROP COLUMN IF EXISTS economic_number,
    DROP COLUMN IF EXISTS box_economic_number;

-- 2. Standardize wms.warehouse_outbounds
ALTER TABLE wms.warehouse_outbounds
    ADD COLUMN IF NOT EXISTS no_eco_tractor VARCHAR(100),
    ADD COLUMN IF NOT EXISTS no_eco_caja VARCHAR(100);

UPDATE wms.warehouse_outbounds
SET no_eco_tractor = economic_number
WHERE (no_eco_tractor IS NULL OR no_eco_tractor = '') AND economic_number IS NOT NULL AND economic_number <> '';

UPDATE wms.warehouse_outbounds
SET no_eco_caja = box_economic_number
WHERE (no_eco_caja IS NULL OR no_eco_caja = '') AND box_economic_number IS NOT NULL AND box_economic_number <> '';

ALTER TABLE wms.warehouse_outbounds
    DROP COLUMN IF EXISTS economic_number,
    DROP COLUMN IF EXISTS box_economic_number;
