-- ==============================================================================
-- 4GUARD WMS - FLYWAY MIGRATION V48
-- Descripcion: Flexibilizacion de montacarguistas y vinculacion directa con cuentas de usuario.
--              - license_number_dc3 y license_expiration_date pasan a ser opcionales (NULL).
--              - Se agrega user_id UUID REFERENCES wms.users(id) para acceso a Terminal RF.
--              - Constraint unico parcial para DC-3 (solo si no es nulo).
-- ==============================================================================

SET search_path TO wms, public;

-- 1. Flexibilizar campos de certificacion DC-3
ALTER TABLE wms.forklift_operators ALTER COLUMN license_number_dc3 DROP NOT NULL;
ALTER TABLE wms.forklift_operators ALTER COLUMN license_expiration_date DROP NOT NULL;

-- 2. Eliminar constraint unico rigido de DC-3 y reemplazarlo por indice parcial unico
ALTER TABLE wms.forklift_operators DROP CONSTRAINT IF EXISTS uk_forklift_operator_dc3;

DROP INDEX IF EXISTS wms.uk_forklift_operator_dc3;
CREATE UNIQUE INDEX IF NOT EXISTS uk_forklift_operator_dc3 
    ON wms.forklift_operators (organization_id, license_number_dc3) 
    WHERE license_number_dc3 IS NOT NULL AND is_deleted = FALSE;

-- 3. Vincular con wms.users para credenciales de acceso directo
ALTER TABLE wms.forklift_operators ADD COLUMN IF NOT EXISTS user_id UUID REFERENCES wms.users(id) ON DELETE SET NULL;

DROP INDEX IF EXISTS wms.uk_forklift_operators_user_id;
CREATE UNIQUE INDEX IF NOT EXISTS uk_forklift_operators_user_id 
    ON wms.forklift_operators (user_id) 
    WHERE user_id IS NOT NULL;

COMMENT ON COLUMN wms.forklift_operators.user_id IS 'FK a wms.users(id). Cuenta de acceso vinculada para Terminal RF / Tableta.';
