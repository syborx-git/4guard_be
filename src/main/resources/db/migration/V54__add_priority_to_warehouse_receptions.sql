-- V54: Adición de columna priority en wms.warehouse_receptions para soporte de Preemptive Priority Queue
ALTER TABLE wms.warehouse_receptions
    ADD COLUMN IF NOT EXISTS priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL';

CREATE INDEX IF NOT EXISTS idx_wr_priority ON wms.warehouse_receptions(priority);

