-- =============================================================================
-- 4GUARD WMS — Flyway Migration V19
-- Description: 
-- 1. Fix PostgreSQL trigger wms.update_updated_at_column() so it only updates
--    NEW.updated_at without altering NEW.version (Hibernate @Version manages optimistic locking).
-- 2. Add performance composite indexes for inventory lookups and floor management.
-- =============================================================================

SET search_path TO wms, public;

-- 1. Redefinir la función de actualización de timestamp sin alterar la columna 'version'
CREATE OR REPLACE FUNCTION wms.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    -- Eliminado: NEW.version = OLD.version + 1; (Gestionado exclusivamente por Hibernate @Version)
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 2. Índices de rendimiento compuestos para optimización de consultas de almacén
CREATE INDEX IF NOT EXISTS idx_inventory_branch_loc_state
    ON wms.inventory_items (branch_id, location_id, state);

CREATE INDEX IF NOT EXISTS idx_inventory_client_sku
    ON wms.inventory_items (client_id, sku_id);

CREATE INDEX IF NOT EXISTS idx_inventory_sap_folio
    ON wms.inventory_items (sap_folio);
