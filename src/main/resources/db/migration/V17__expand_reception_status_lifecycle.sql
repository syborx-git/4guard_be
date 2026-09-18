-- =============================================================================
-- 4GUARD WMS — Flyway Migration V17
-- Description: Expand warehouse receptions status lifecycle constraint to support
--              the full decoupled operational flow (Caseta -> Admin -> Forklift -> Audit).
-- Lifecycle states: REGISTERED -> ASSIGNED -> IN_PROGRESS -> DISCHARGED -> COMPLETED (or CANCELLED)
-- =============================================================================

ALTER TABLE wms.warehouse_receptions DROP CONSTRAINT IF EXISTS chk_wr_status;

ALTER TABLE wms.warehouse_receptions ADD CONSTRAINT chk_wr_status 
    CHECK (status IN ('REGISTERED', 'ASSIGNED', 'IN_PROGRESS', 'DISCHARGED', 'COMPLETED', 'CANCELLED'));

COMMENT ON COLUMN wms.warehouse_receptions.status IS 
'Lifecycle status: REGISTERED (Caseta), ASSIGNED (Ramp/Forklift assigned), IN_PROGRESS (Unloading), DISCHARGED (Finished unloading), COMPLETED (Finalized), CANCELLED (Cancelled)';
