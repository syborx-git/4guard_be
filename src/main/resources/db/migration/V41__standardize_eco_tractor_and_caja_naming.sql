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

DO $$
BEGIN
    -- 1. Standardize wms.security_pre_checkins
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='wms' AND table_name='security_pre_checkins' AND column_name='no_eco_caja') THEN
        ALTER TABLE wms.security_pre_checkins ADD COLUMN no_eco_caja VARCHAR(100);
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='wms' AND table_name='security_pre_checkins' AND column_name='economic_number') THEN
        EXECUTE 'UPDATE wms.security_pre_checkins SET no_eco_tractor = economic_number WHERE (no_eco_tractor IS NULL OR no_eco_tractor = '''') AND economic_number IS NOT NULL AND economic_number <> ''''';
        ALTER TABLE wms.security_pre_checkins DROP COLUMN economic_number;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='wms' AND table_name='security_pre_checkins' AND column_name='box_economic_number') THEN
        EXECUTE 'UPDATE wms.security_pre_checkins SET no_eco_caja = box_economic_number WHERE (no_eco_caja IS NULL OR no_eco_caja = '''') AND box_economic_number IS NOT NULL AND box_economic_number <> ''''';
        ALTER TABLE wms.security_pre_checkins DROP COLUMN box_economic_number;
    END IF;

    -- 2. Standardize wms.warehouse_outbounds
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='wms' AND table_name='warehouse_outbounds' AND column_name='no_eco_tractor') THEN
        ALTER TABLE wms.warehouse_outbounds ADD COLUMN no_eco_tractor VARCHAR(100);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='wms' AND table_name='warehouse_outbounds' AND column_name='no_eco_caja') THEN
        ALTER TABLE wms.warehouse_outbounds ADD COLUMN no_eco_caja VARCHAR(100);
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='wms' AND table_name='warehouse_outbounds' AND column_name='economic_number') THEN
        EXECUTE 'UPDATE wms.warehouse_outbounds SET no_eco_tractor = economic_number WHERE (no_eco_tractor IS NULL OR no_eco_tractor = '''') AND economic_number IS NOT NULL AND economic_number <> ''''';
        ALTER TABLE wms.warehouse_outbounds DROP COLUMN economic_number;
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='wms' AND table_name='warehouse_outbounds' AND column_name='box_economic_number') THEN
        EXECUTE 'UPDATE wms.warehouse_outbounds SET no_eco_caja = box_economic_number WHERE (no_eco_caja IS NULL OR no_eco_caja = '''') AND box_economic_number IS NOT NULL AND box_economic_number <> ''''';
        ALTER TABLE wms.warehouse_outbounds DROP COLUMN box_economic_number;
    END IF;
END $$;
