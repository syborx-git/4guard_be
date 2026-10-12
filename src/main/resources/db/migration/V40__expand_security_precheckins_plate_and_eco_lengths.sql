-- ==============================================================================
-- Flyway Migration: V40__expand_security_precheckins_plate_and_eco_lengths.sql
-- Description: Expands plate, economic number, dimensions, and license column lengths
--              in wms.security_pre_checkins to prevent truncation exceptions
--              when checking out transport units from outbound/reception flows.
-- ==============================================================================

SET search_path TO wms, public;

ALTER TABLE wms.security_pre_checkins
    ALTER COLUMN tractor_plates TYPE VARCHAR(50),
    ALTER COLUMN box_plates TYPE VARCHAR(50),
    ALTER COLUMN economic_number TYPE VARCHAR(100),
    ALTER COLUMN box_economic_number TYPE VARCHAR(100),
    ALTER COLUMN no_eco_tractor TYPE VARCHAR(100),
    ALTER COLUMN box_dimensions TYPE VARCHAR(50),
    ALTER COLUMN driver_license TYPE VARCHAR(100);
