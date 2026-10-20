-- ==============================================================================
-- Flyway Migration: V40__expand_security_precheckins_plate_and_eco_lengths.sql
-- Description: Expands plate, economic number, dimensions, and license column lengths
--              in wms.security_pre_checkins to prevent truncation exceptions
--              when checking out transport units from outbound/reception flows.
-- ==============================================================================

SET search_path TO wms, public;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='wms' AND table_name='security_pre_checkins' AND column_name='tractor_plates') THEN
        ALTER TABLE wms.security_pre_checkins ALTER COLUMN tractor_plates TYPE VARCHAR(50);
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='wms' AND table_name='security_pre_checkins' AND column_name='box_plates') THEN
        ALTER TABLE wms.security_pre_checkins ALTER COLUMN box_plates TYPE VARCHAR(50);
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='wms' AND table_name='security_pre_checkins' AND column_name='economic_number') THEN
        ALTER TABLE wms.security_pre_checkins ALTER COLUMN economic_number TYPE VARCHAR(100);
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='wms' AND table_name='security_pre_checkins' AND column_name='box_economic_number') THEN
        ALTER TABLE wms.security_pre_checkins ALTER COLUMN box_economic_number TYPE VARCHAR(100);
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='wms' AND table_name='security_pre_checkins' AND column_name='no_eco_tractor') THEN
        ALTER TABLE wms.security_pre_checkins ALTER COLUMN no_eco_tractor TYPE VARCHAR(100);
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='wms' AND table_name='security_pre_checkins' AND column_name='box_dimensions') THEN
        ALTER TABLE wms.security_pre_checkins ALTER COLUMN box_dimensions TYPE VARCHAR(50);
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='wms' AND table_name='security_pre_checkins' AND column_name='driver_license') THEN
        ALTER TABLE wms.security_pre_checkins ALTER COLUMN driver_license TYPE VARCHAR(100);
    END IF;
END $$;
