-- ==============================================================================
-- Flyway Migration: V33__expand_inventory_items_sscc_and_ua_lengths.sql
-- Description: Expands sscc, external_ua, pallet_code, and batch_number column lengths
--              to VARCHAR(100) to support long supplier barcodes, GS1-128 strings,
--              and custom alphanumeric UA codes without truncation errors.
-- ==============================================================================

-- 1. Expand columns in wms.inventory_items
ALTER TABLE wms.inventory_items
    ALTER COLUMN sscc TYPE VARCHAR(100),
    ALTER COLUMN external_ua TYPE VARCHAR(100),
    ALTER COLUMN batch_number TYPE VARCHAR(100),
    ALTER COLUMN sap_folio TYPE VARCHAR(100);

-- 2. Expand columns in wms.warehouse_reception_pallets
ALTER TABLE wms.warehouse_reception_pallets
    ALTER COLUMN pallet_code TYPE VARCHAR(100),
    ALTER COLUMN supplier_ua_code TYPE VARCHAR(100),
    ALTER COLUMN internal_ua_code TYPE VARCHAR(100),
    ALTER COLUMN lot_number TYPE VARCHAR(100);

-- 3. Expand columns in wms.ua_mappings
ALTER TABLE wms.ua_mappings
    ALTER COLUMN supplier_ua_code TYPE VARCHAR(100),
    ALTER COLUMN internal_ua_code TYPE VARCHAR(100);

-- 4. Expand columns in wms.inventory_audit_log
ALTER TABLE wms.inventory_audit_log
    ALTER COLUMN pallet_code TYPE VARCHAR(100),
    ALTER COLUMN remision_folio TYPE VARCHAR(100);
