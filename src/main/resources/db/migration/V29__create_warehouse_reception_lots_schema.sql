-- =============================================================================
-- V29: Create Warehouse Reception Lots Schema (Multi-Lot Support for Inbound F01)
-- Author: 4GUARD Engineering Team
-- Description: Creates wms.warehouse_reception_lots to support multiple lots
--              under a single reception remission, linking reception pallets.
-- =============================================================================

SET search_path TO wms, public;

-- 1. Tabla para soportar múltiples lotes por remisión en una recepción
CREATE TABLE IF NOT EXISTS wms.warehouse_reception_lots (
    id                          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id             UUID NOT NULL REFERENCES wms.organizations(id),
    branch_id                   UUID NOT NULL REFERENCES wms.branches(id),
    reception_id                UUID NOT NULL REFERENCES wms.warehouse_receptions(id) ON DELETE CASCADE,
    
    lot_number                  VARCHAR(50) NOT NULL,
    sku_id                      UUID REFERENCES wms.products_sku(id),
    
    elaboration_date            DATE,
    expiration_date             DATE NOT NULL,
    shelf_life_days_remaining   INTEGER,
    shelf_life_status           VARCHAR(30) NOT NULL DEFAULT 'COMPLIANT'
                                    CONSTRAINT chk_wrl_shelf_life CHECK (shelf_life_status IN ('COMPLIANT', 'WARNING', 'NON_COMPLIANT')),
    
    status                      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                                    CONSTRAINT chk_wrl_status CHECK (status IN ('ACTIVE', 'COMPLETED', 'CANCELLED')),
    observations                TEXT,
    
    version                     BIGINT NOT NULL DEFAULT 1,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by                  VARCHAR(36),
    updated_by                  VARCHAR(36),

    CONSTRAINT uk_wr_reception_lot UNIQUE (reception_id, lot_number)
);

CREATE INDEX IF NOT EXISTS idx_wrl_reception ON wms.warehouse_reception_lots(reception_id);
CREATE INDEX IF NOT EXISTS idx_wrl_lot_number ON wms.warehouse_reception_lots(lot_number);
CREATE INDEX IF NOT EXISTS idx_wrl_exp_date ON wms.warehouse_reception_lots(expiration_date);

-- 2. Vincular tabla de tarimas con su lote correspondiente preservando reception_id (NOT NULL)
ALTER TABLE wms.warehouse_reception_pallets
    ADD COLUMN IF NOT EXISTS reception_lot_id UUID REFERENCES wms.warehouse_reception_lots(id);

CREATE INDEX IF NOT EXISTS idx_wrp_reception_lot ON wms.warehouse_reception_pallets(reception_lot_id);

-- 3. Migración de compatibilidad para recepciones históricas con lot_number
INSERT INTO wms.warehouse_reception_lots (
    id, organization_id, branch_id, reception_id, lot_number, sku_id,
    elaboration_date, expiration_date, shelf_life_days_remaining, shelf_life_status,
    status, created_at, updated_at
)
SELECT 
    uuid_generate_v4(),
    wr.organization_id,
    wr.branch_id,
    wr.id,
    COALESCE(NULLIF(wr.lot_number, ''), 'LOT-' || wr.folio),
    wr.sku_id,
    wr.elaboration_date,
    COALESCE(wr.expiration_date, CURRENT_DATE + INTERVAL '365 days'),
    COALESCE(wr.shelf_life_days_remaining, 365),
    COALESCE(wr.shelf_life_status, 'COMPLIANT'),
    'ACTIVE',
    wr.created_at,
    wr.updated_at
FROM wms.warehouse_receptions wr
WHERE NOT EXISTS (
    SELECT 1 FROM wms.warehouse_reception_lots l WHERE l.reception_id = wr.id
);

-- Vincular tarimas históricas al lote correspondiente
UPDATE wms.warehouse_reception_pallets p
SET reception_lot_id = l.id
FROM wms.warehouse_reception_lots l
WHERE l.reception_id = p.reception_id
  AND p.reception_lot_id IS NULL;
