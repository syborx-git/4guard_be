-- =============================================================================
-- 4GUARD WMS — Flyway Migration V47
-- Description: Dynamic Bay Capacity & Real-Time Section Capacity Recalculation
-- Ensures each warehouse position (bay) stores its user-configured pallet capacity
-- and automatically maintains wms.warehouse_sections.capacidad_tarimas in sync.
-- =============================================================================

SET search_path TO wms, public;

-- 1. Asegurar restricción de capacidad positiva en wms.locations
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'chk_locations_capacity_units_positive'
    ) THEN
        ALTER TABLE wms.locations
            ADD CONSTRAINT chk_locations_capacity_units_positive
            CHECK (capacity_units IS NULL OR capacity_units > 0);
    END IF;
END $$;

-- 2. Función Trigger: Recalcula capacidad_tarimas en wms.warehouse_sections
CREATE OR REPLACE FUNCTION wms.fn_sync_warehouse_section_capacity()
RETURNS TRIGGER AS $$
DECLARE
    v_section_id UUID;
    v_total_capacity INTEGER;
BEGIN
    -- Determinar el section_id afectado
    IF TG_OP = 'DELETE' THEN
        v_section_id := OLD.section_id;
    ELSE
        v_section_id := NEW.section_id;
    END IF;

    IF v_section_id IS NOT NULL THEN
        -- Calcular la suma de capacidad_units de todas las posiciones operativas activas de la sección
        SELECT COALESCE(SUM(capacity_units), 0)
          INTO v_total_capacity
          FROM wms.locations
         WHERE section_id = v_section_id
           AND (status IS NULL OR status != 'MAINTENANCE');

        UPDATE wms.warehouse_sections
           SET capacidad_tarimas = v_total_capacity,
               updated_at = CURRENT_TIMESTAMP
         WHERE id = v_section_id;
    END IF;

    -- Si hubo cambio de sección (UPDATE cambiando de section_id), actualizar también la sección anterior
    IF TG_OP = 'UPDATE' AND OLD.section_id IS NOT NULL AND OLD.section_id IS DISTINCT FROM NEW.section_id THEN
        SELECT COALESCE(SUM(capacity_units), 0)
          INTO v_total_capacity
          FROM wms.locations
         WHERE section_id = OLD.section_id
           AND (status IS NULL OR status != 'MAINTENANCE');

        UPDATE wms.warehouse_sections
           SET capacidad_tarimas = v_total_capacity,
               updated_at = CURRENT_TIMESTAMP
         WHERE id = OLD.section_id;
    END IF;

    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

-- 3. Crear el Trigger en wms.locations
DROP TRIGGER IF EXISTS trg_sync_warehouse_section_capacity ON wms.locations;

CREATE TRIGGER trg_sync_warehouse_section_capacity
AFTER INSERT OR UPDATE OF capacity_units, section_id, status OR DELETE
ON wms.locations
FOR EACH ROW
EXECUTE FUNCTION wms.fn_sync_warehouse_section_capacity();

-- 4. Sincronización inicial de capacidad_tarimas para todas las secciones existentes
UPDATE wms.warehouse_sections ws
   SET capacidad_tarimas = COALESCE((
       SELECT SUM(loc.capacity_units)
         FROM wms.locations loc
        WHERE loc.section_id = ws.id
          AND (loc.status IS NULL OR loc.status != 'MAINTENANCE')
   ), ws.capacidad_tarimas),
   updated_at = CURRENT_TIMESTAMP;
