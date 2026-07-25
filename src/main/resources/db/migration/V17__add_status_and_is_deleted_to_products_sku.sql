-- =============================================================================
-- V17: Add status and is_deleted columns to wms.products_sku
-- Description: Supports active/inactive status and soft deletion.
-- =============================================================================

ALTER TABLE wms.products_sku ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE';
ALTER TABLE wms.products_sku ADD COLUMN IF NOT EXISTS is_deleted BOOLEAN DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_products_sku_client_deleted ON wms.products_sku (client_id, is_deleted);
