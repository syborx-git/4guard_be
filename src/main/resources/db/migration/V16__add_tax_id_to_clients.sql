-- =============================================================================
-- V16: Add tax_id (RFC) column to wms.clients
-- Description: Adds RFC/Tax ID support to Client entity with org index.
-- =============================================================================

ALTER TABLE wms.clients ADD COLUMN IF NOT EXISTS tax_id VARCHAR(30);

CREATE INDEX IF NOT EXISTS idx_clients_org_tax_id ON wms.clients (organization_id, tax_id);
