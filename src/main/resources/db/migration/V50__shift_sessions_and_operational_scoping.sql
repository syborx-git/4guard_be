-- ============================================================================
-- V50: Sesiones operativas de turno y scoping de movimientos
-- Aditivo: no modifica migraciones previas ni borra datos.
-- ============================================================================

CREATE TABLE IF NOT EXISTS wms.shift_sessions (
    id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id   UUID NOT NULL REFERENCES wms.organizations(id),
    branch_id         UUID NOT NULL REFERENCES wms.branches(id),
    shift_id          UUID NULL REFERENCES wms.wms_shifts(id) ON DELETE SET NULL,
    business_date     DATE NOT NULL,
    started_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ended_at          TIMESTAMPTZ NULL,
    status            VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    source            VARCHAR(20) NOT NULL DEFAULT 'AUTO',
    opened_by         UUID NULL REFERENCES wms.users(id) ON DELETE SET NULL,
    closed_by         UUID NULL REFERENCES wms.users(id) ON DELETE SET NULL,
    CONSTRAINT chk_shift_session_status CHECK (status IN ('OPEN', 'CLOSED')),
    CONSTRAINT chk_shift_session_source CHECK (source IN ('AUTO', 'MANUAL', 'FALLBACK'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_shift_sessions_open_branch
    ON wms.shift_sessions(branch_id)
    WHERE status = 'OPEN';

CREATE INDEX IF NOT EXISTS idx_shift_sessions_org_branch_date
    ON wms.shift_sessions(organization_id, branch_id, business_date);

CREATE INDEX IF NOT EXISTS idx_shift_sessions_started_at
    ON wms.shift_sessions(started_at DESC);

ALTER TABLE wms.warehouse_receptions
    ADD COLUMN IF NOT EXISTS shift_session_id UUID NULL REFERENCES wms.shift_sessions(id) ON DELETE SET NULL;

ALTER TABLE wms.warehouse_outbounds
    ADD COLUMN IF NOT EXISTS shift_session_id UUID NULL REFERENCES wms.shift_sessions(id) ON DELETE SET NULL;

ALTER TABLE wms.warehouse_transfers
    ADD COLUMN IF NOT EXISTS shift_session_id UUID NULL REFERENCES wms.shift_sessions(id) ON DELETE SET NULL;

ALTER TABLE wms.audit_logs
    ADD COLUMN IF NOT EXISTS shift_session_id UUID NULL REFERENCES wms.shift_sessions(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_wr_shift_session ON wms.warehouse_receptions(shift_session_id);
CREATE INDEX IF NOT EXISTS idx_wo_shift_session ON wms.warehouse_outbounds(shift_session_id);
CREATE INDEX IF NOT EXISTS idx_wt_shift_session ON wms.warehouse_transfers(shift_session_id);
CREATE INDEX IF NOT EXISTS idx_audit_shift_session ON wms.audit_logs(shift_session_id);
