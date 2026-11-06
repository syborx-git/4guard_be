-- V56: Expansión de ciclo de vida y prioridad en wms.warehouse_outbounds
-- Soporte para despacho asíncrono a Terminal RF, Preemptive Priority Queue y Corte de Turno (ADR-011, ADR-016, ADR-019, ADR-020, ADR-022)

-- 1. Ampliar el CHECK constraint de estatus para admitir PAUSED
ALTER TABLE wms.warehouse_outbounds DROP CONSTRAINT IF EXISTS chk_wo_status;
ALTER TABLE wms.warehouse_outbounds ADD CONSTRAINT chk_wo_status
    CHECK (status IN ('DRAFT', 'REGISTERED', 'ASSIGNED', 'IN_PROGRESS', 'PAUSED', 'LOADED', 'COMPLETED', 'CANCELLED'));

-- 2. Adición de columna priority para soporte de misiones urgentes
ALTER TABLE wms.warehouse_outbounds
    ADD COLUMN IF NOT EXISTS priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_wo_priority'
    ) THEN
        ALTER TABLE wms.warehouse_outbounds ADD CONSTRAINT chk_wo_priority
            CHECK (priority IN ('NORMAL', 'URGENT'));
    END IF;
END $$;

-- 3. Adición de columnas de tiempos de maniobra y justificación de pausa
ALTER TABLE wms.warehouse_outbounds ADD COLUMN IF NOT EXISTS started_at TIMESTAMPTZ;
ALTER TABLE wms.warehouse_outbounds ADD COLUMN IF NOT EXISTS completed_at TIMESTAMPTZ;
ALTER TABLE wms.warehouse_outbounds ADD COLUMN IF NOT EXISTS paused_at TIMESTAMPTZ;
ALTER TABLE wms.warehouse_outbounds ADD COLUMN IF NOT EXISTS pause_reason VARCHAR(100);

-- 4. Índices de rendimiento para consultas de tablero por turno y prioridad
CREATE INDEX IF NOT EXISTS idx_wo_priority ON wms.warehouse_outbounds(priority);
CREATE INDEX IF NOT EXISTS idx_wo_shift_status ON wms.warehouse_outbounds(shift_session_id, status);

