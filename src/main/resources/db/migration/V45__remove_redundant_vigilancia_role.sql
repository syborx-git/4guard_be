-- ==============================================================================
-- 4GUARD WMS - FLYWAY MIGRATION V45
-- Descripcion: Depuracion de roles de seguridad redundantes.
--             Se reasignan preventivamente usuarios de VIGILANCIA a SECURITY_GUARD
--             y se elimina el rol VIGILANCIA junto con sus asignaciones de permisos.
-- ==============================================================================

-- 1. Reasignar cualquier usuario que tenga el rol VIGILANCIA al rol oficial SECURITY_GUARD
DO $$
DECLARE
    v_security_guard_id UUID;
    v_vigilancia_id UUID;
BEGIN
    SELECT id INTO v_security_guard_id FROM wms.roles WHERE name = 'SECURITY_GUARD';
    SELECT id INTO v_vigilancia_id FROM wms.roles WHERE name = 'VIGILANCIA';

    IF v_vigilancia_id IS NOT NULL AND v_security_guard_id IS NOT NULL THEN
        -- Reasignar usuarios si existiese alguno
        UPDATE wms.users
        SET role_id = v_security_guard_id
        WHERE role_id = v_vigilancia_id;

        -- Eliminar permisos asociados a VIGILANCIA
        DELETE FROM wms.role_permissions
        WHERE role_id = v_vigilancia_id;

        -- Eliminar el rol VIGILANCIA
        DELETE FROM wms.roles
        WHERE id = v_vigilancia_id;
    END IF;
END $$;
