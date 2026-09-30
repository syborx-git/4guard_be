-- =================================================================================================
-- FLYWAY MIGRATION V31: Add driver_phone (10 Digits Standard) to wms.security_pre_checkins
-- =================================================================================================

ALTER TABLE wms.security_pre_checkins
    ADD COLUMN IF NOT EXISTS driver_phone VARCHAR(10);

-- Constraint: Phone must be 10 numeric digits if provided
ALTER TABLE wms.security_pre_checkins
    DROP CONSTRAINT IF EXISTS chk_security_precheckins_driver_phone;

ALTER TABLE wms.security_pre_checkins
    ADD CONSTRAINT chk_security_precheckins_driver_phone
    CHECK (driver_phone ~ '^[0-9]{10}$' OR driver_phone IS NULL);

COMMENT ON COLUMN wms.security_pre_checkins.driver_phone IS 'Driver 10-digit mobile number for security gate verification';
