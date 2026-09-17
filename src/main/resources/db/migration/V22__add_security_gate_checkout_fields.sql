-- =================================================================================================
-- FLYWAY MIGRATION V22: Security Gate Exit Check-Out and Departure Audit Fields
-- =================================================================================================

ALTER TABLE wms.security_pre_checkins
    ADD COLUMN IF NOT EXISTS exit_observations TEXT,
    ADD COLUMN IF NOT EXISTS exit_seal_numbers TEXT[],
    ADD COLUMN IF NOT EXISTS exited_by VARCHAR(100),
    ADD COLUMN IF NOT EXISTS exited_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(100),
    ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0,
    ADD COLUMN IF NOT EXISTS no_eco_tractor VARCHAR(100);

-- Fast index on generated folio
CREATE INDEX IF NOT EXISTS idx_security_precheckins_gen_folio ON wms.security_pre_checkins(generated_folio);
