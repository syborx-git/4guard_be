-- =============================================================================
-- V52: Add shift_session_id to wms.security_pre_checkins
-- Links gate check-out records and driver passes to warehouse operational shift sessions
-- =============================================================================

ALTER TABLE wms.security_pre_checkins
    ADD COLUMN IF NOT EXISTS shift_session_id UUID NULL REFERENCES wms.shift_sessions(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_precheckin_shift_session
    ON wms.security_pre_checkins(shift_session_id);

COMMENT ON COLUMN wms.security_pre_checkins.shift_session_id IS
    'Identificador de la sesión de turno operativo en la que se registró la salida o pase de caseta.';

