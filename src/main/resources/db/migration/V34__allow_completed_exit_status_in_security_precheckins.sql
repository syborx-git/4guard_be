-- ==============================================================================
-- Flyway Migration: V34__allow_completed_exit_status_in_security_precheckins.sql
-- Description: Updates chk_security_precheckins_status check constraint to include
--              'COMPLETED_EXIT' for transport gate exit checkout lifecycle.
-- ==============================================================================

SET search_path TO wms, public;

ALTER TABLE wms.security_pre_checkins
    DROP CONSTRAINT IF EXISTS chk_security_precheckins_status;

ALTER TABLE wms.security_pre_checkins
    ADD CONSTRAINT chk_security_precheckins_status
    CHECK (status IN ('PENDING_DRIVER', 'SUBMITTED', 'COMPLETED', 'COMPLETED_EXIT', 'CANCELLED'));
