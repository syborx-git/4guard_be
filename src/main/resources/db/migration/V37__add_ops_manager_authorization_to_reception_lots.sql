-- ============================================================================
-- Migration: V37__add_ops_manager_authorization_to_reception_lots.sql
-- Description: Adds Operations Manager authorization and justification fields
--              for lots with shelf life < 9 months (270 days) to warehouse_reception_lots
--              and warehouse_receptions tables.
-- ============================================================================

-- 1. Alter wms.warehouse_reception_lots
ALTER TABLE wms.warehouse_reception_lots
    ADD COLUMN IF NOT EXISTS requires_ops_authorization BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS authorized_by_ops_manager VARCHAR(120),
    ADD COLUMN IF NOT EXISTS ops_manager_reason TEXT,
    ADD COLUMN IF NOT EXISTS ops_authorization_date TIMESTAMP WITH TIME ZONE;

-- 2. Alter wms.warehouse_receptions
ALTER TABLE wms.warehouse_receptions
    ADD COLUMN IF NOT EXISTS requires_ops_authorization BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS authorized_by_ops_manager VARCHAR(120),
    ADD COLUMN IF NOT EXISTS ops_manager_reason TEXT,
    ADD COLUMN IF NOT EXISTS ops_authorization_date TIMESTAMP WITH TIME ZONE;

-- 3. Create index for authorized lots filtering
CREATE INDEX IF NOT EXISTS idx_wrl_ops_auth ON wms.warehouse_reception_lots(requires_ops_authorization);
