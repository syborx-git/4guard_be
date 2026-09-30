-- ==============================================================================
-- Flyway Migration: V28__reception_shelf_life_ua_mapping_and_audit.sql
-- Description: Adds shelf life tracking, immutable UA mappings, and inventory audit logs.
-- ==============================================================================

-- 1. Extend warehouse_receptions with shelf life fields
ALTER TABLE wms.warehouse_receptions
    ADD COLUMN IF NOT EXISTS shelf_life_days_remaining BIGINT,
    ADD COLUMN IF NOT EXISTS shelf_life_status VARCHAR(40) DEFAULT 'PENDING';

-- 2. Extend warehouse_reception_pallets with dual-UA and pallet-level lot data
ALTER TABLE wms.warehouse_reception_pallets
    ADD COLUMN IF NOT EXISTS supplier_ua_code VARCHAR(60),
    ADD COLUMN IF NOT EXISTS internal_ua_code VARCHAR(60),
    ADD COLUMN IF NOT EXISTS is_ua_relabelled BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS lot_number VARCHAR(50),
    ADD COLUMN IF NOT EXISTS expiration_date DATE;

-- Populate supplier_ua_code from pallet_code for existing records
UPDATE wms.warehouse_reception_pallets
SET supplier_ua_code = pallet_code
WHERE supplier_ua_code IS NULL AND pallet_code IS NOT NULL;

-- 3. Table: wms.ua_mappings (Immutable mapping of original supplier UA to internal SSCC)
CREATE TABLE IF NOT EXISTS wms.ua_mappings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES wms.organizations(id),
    reception_id UUID NOT NULL REFERENCES wms.warehouse_receptions(id) ON DELETE CASCADE,
    pallet_id UUID NOT NULL REFERENCES wms.warehouse_reception_pallets(id) ON DELETE CASCADE,
    supplier_ua_code VARCHAR(60) NOT NULL,
    internal_ua_code VARCHAR(60) NOT NULL,
    relabelled_by VARCHAR(100) NOT NULL,
    relabelled_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reason VARCHAR(255) DEFAULT 'Re-etiquetado selectivo a estándar 4Guard SSCC GS1-128',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ua_mappings_reception ON wms.ua_mappings(reception_id);
CREATE INDEX IF NOT EXISTS idx_ua_mappings_pallet ON wms.ua_mappings(pallet_id);
CREATE INDEX IF NOT EXISTS idx_ua_mappings_internal_ua ON wms.ua_mappings(internal_ua_code);
CREATE INDEX IF NOT EXISTS idx_ua_mappings_supplier_ua ON wms.ua_mappings(supplier_ua_code);

-- 4. Table: wms.inventory_audit_log (Granular lifecycle events per pallet & remission tree)
CREATE TABLE IF NOT EXISTS wms.inventory_audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES wms.organizations(id),
    pallet_id UUID REFERENCES wms.warehouse_reception_pallets(id) ON DELETE SET NULL,
    inventory_item_id UUID REFERENCES wms.inventory_items(id) ON DELETE SET NULL,
    pallet_code VARCHAR(60) NOT NULL,
    remision_folio VARCHAR(60),
    event_type VARCHAR(60) NOT NULL,
    source_location VARCHAR(100),
    target_location VARCHAR(100),
    performed_by VARCHAR(100) NOT NULL,
    performed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reason TEXT,
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_inv_audit_org ON wms.inventory_audit_log(organization_id);
CREATE INDEX IF NOT EXISTS idx_inv_audit_pallet_code ON wms.inventory_audit_log(pallet_code);
CREATE INDEX IF NOT EXISTS idx_inv_audit_remision ON wms.inventory_audit_log(remision_folio);
CREATE INDEX IF NOT EXISTS idx_inv_audit_event_type ON wms.inventory_audit_log(event_type);
CREATE INDEX IF NOT EXISTS idx_inv_audit_performed_at ON wms.inventory_audit_log(performed_at);
