-- V55: Expansión de ciclo de vida y prioridad en wms.warehouse_transfers
-- Soporte para despacho asíncrono a Terminal RF, Preemptive Priority Queue y Corte de Turno (ADR-011, ADR-017, ADR-020, ADR-022)

-- 1. Ampliar el CHECK constraint de estatus para admitir ASSIGNED, IN_PROGRESS y PAUSED
ALTER TABLE wms.warehouse_transfers DROP CONSTRAINT IF EXISTS chk_wt_status;
ALTER TABLE wms.warehouse_transfers ADD CONSTRAINT chk_wt_status
    CHECK (status IN ('DRAFT', 'ASSIGNED', 'IN_PROGRESS', 'PAUSED', 'COMPLETED', 'CANCELLED'));

-- 2. Cambiar default de status a ASSIGNED (cuando se despacha a la terminal RF)
ALTER TABLE wms.warehouse_transfers ALTER COLUMN status SET DEFAULT 'ASSIGNED';

-- 3. Adición de columna priority para soporte de misiones urgentes
ALTER TABLE wms.warehouse_transfers
    ADD COLUMN IF NOT EXISTS priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_wt_priority'
    ) THEN
        ALTER TABLE wms.warehouse_transfers ADD CONSTRAINT chk_wt_priority
            CHECK (priority IN ('NORMAL', 'URGENT'));
    END IF;
END $$;

-- 4. Adición de columnas de tiempos de maniobra y justificación de pausa
ALTER TABLE wms.warehouse_transfers ADD COLUMN IF NOT EXISTS started_at TIMESTAMPTZ;
ALTER TABLE wms.warehouse_transfers ADD COLUMN IF NOT EXISTS completed_at TIMESTAMPTZ;
ALTER TABLE wms.warehouse_transfers ADD COLUMN IF NOT EXISTS paused_at TIMESTAMPTZ;
ALTER TABLE wms.warehouse_transfers ADD COLUMN IF NOT EXISTS pause_reason VARCHAR(100);

-- 5. Índices de rendimiento para consultas de tablero por turno y prioridad
CREATE INDEX IF NOT EXISTS idx_wt_priority ON wms.warehouse_transfers(priority);
CREATE INDEX IF NOT EXISTS idx_wt_status ON wms.warehouse_transfers(status);
CREATE INDEX IF NOT EXISTS idx_wt_shift_status ON wms.warehouse_transfers(shift_session_id, status);

