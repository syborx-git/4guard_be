-- V38: Add Reentry Logistics and Multicycle Traceability Schema to Warehouse Receptions and Security Pre-checkins

-- 1. Alter warehouse_receptions table
ALTER TABLE wms.warehouse_receptions
    ADD COLUMN IF NOT EXISTS operation_type VARCHAR(30) DEFAULT 'ENTRY',
    ADD COLUMN IF NOT EXISTS source_outbound_id UUID REFERENCES wms.warehouse_outbounds(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS source_outbound_folio VARCHAR(50),
    ADD COLUMN IF NOT EXISTS reentry_reason VARCHAR(100),
    ADD COLUMN IF NOT EXISTS reentry_notes TEXT;

-- 2. Alter security_pre_checkins table
ALTER TABLE wms.security_pre_checkins
    ADD COLUMN IF NOT EXISTS operation_type VARCHAR(30) DEFAULT 'ENTRY',
    ADD COLUMN IF NOT EXISTS source_outbound_id UUID REFERENCES wms.warehouse_outbounds(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS source_outbound_folio VARCHAR(50),
    ADD COLUMN IF NOT EXISTS reentry_reason VARCHAR(100),
    ADD COLUMN IF NOT EXISTS reentry_notes TEXT;

-- 3. Create indexes for quick lookup
CREATE INDEX IF NOT EXISTS idx_warehouse_receptions_op_type ON wms.warehouse_receptions(operation_type);
CREATE INDEX IF NOT EXISTS idx_warehouse_receptions_src_outbound ON wms.warehouse_receptions(source_outbound_id);
CREATE INDEX IF NOT EXISTS idx_security_precheckins_op_type ON wms.security_pre_checkins(operation_type);
