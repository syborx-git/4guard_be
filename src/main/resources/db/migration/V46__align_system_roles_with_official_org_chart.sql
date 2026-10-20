-- ==============================================================================
-- 4GUARD WMS - FLYWAY MIGRATION V46
-- Descripcion: Depuracion y Homologacion de Roles Oficiales segun Organigrama WMS.
--              Estructura: DG (Admin), GO, CO, SO, MC, LT, M (Montacarguista),
--              mas los roles transversales de Seguridad (SECURITY_GUARD) y Calidad (QM_INSPECTOR).
-- ==============================================================================

SET search_path TO wms, public;

-- 1. Insertar roles faltantes del organigrama oficial si no existen
INSERT INTO wms.roles (id, name, level, is_system)
VALUES 
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'OPERATIONS_COORDINATOR', 5, TRUE),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'QM_INSPECTOR',           4, TRUE)
ON CONFLICT (name) DO NOTHING;

-- Asegurar que el rol CEO / ADMIN tenga nivel 7
UPDATE wms.roles SET level = 7 WHERE name IN ('ADMIN', 'CEO');
UPDATE wms.roles SET level = 6 WHERE name = 'OPERATIONS_MANAGER';
UPDATE wms.roles SET level = 5 WHERE name IN ('OPERATIONS_COORDINATOR', 'OPERATIONS_SUPERVISOR');
UPDATE wms.roles SET level = 4 WHERE name IN ('CONTROL_DESK', 'QM_INSPECTOR');
UPDATE wms.roles SET level = 3 WHERE name = 'SHIFT_LEADER';
UPDATE wms.roles SET level = 2 WHERE name IN ('WAREHOUSE_OPERATOR', 'SECURITY_GUARD');

-- 2. Asignar permisos esenciales a OPERATIONS_COORDINATOR (Hereda permisos operativos similares a Supervisor/Gerente)
INSERT INTO wms.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM wms.roles r
CROSS JOIN wms.permissions p
WHERE r.name = 'OPERATIONS_COORDINATOR'
  AND p.name IN (
      'INVENTORY_READ', 'INVENTORY_CREATE', 'INVENTORY_UPDATE',
      'WAREHOUSE_MOVEMENTS_READ', 'WAREHOUSE_MOVEMENTS_CREATE', 'WAREHOUSE_MOVEMENTS_UPDATE',
      'CARRIERS_READ', 'CLIENTS_READ', 'SUPPLIERS_READ',
      'SECURITY_GATE_READ', 'AUDIT_LOG_READ'
  )
ON CONFLICT DO NOTHING;

-- 3. Asignar permisos esenciales a QM_INSPECTOR
INSERT INTO wms.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM wms.roles r
CROSS JOIN wms.permissions p
WHERE r.name = 'QM_INSPECTOR'
  AND p.name IN (
      'INVENTORY_READ', 'INVENTORY_UPDATE',
      'WAREHOUSE_MOVEMENTS_READ', 'WAREHOUSE_MOVEMENTS_UPDATE',
      'CARRIERS_READ', 'CLIENTS_READ', 'SUPPLIERS_READ',
      'AUDIT_LOG_READ'
  )
ON CONFLICT DO NOTHING;

-- 4. Reasignar usuarios de roles redundantes a los roles oficiales del organigrama:
--    - WAREHOUSE_MANAGER -> OPERATIONS_MANAGER
--    - DOCK_SUPERVISOR   -> OPERATIONS_SUPERVISOR
--    - MANEUVER_OPERATOR -> WAREHOUSE_OPERATOR
DO $$
DECLARE
    v_ops_mgr_id UUID;
    v_wh_mgr_id UUID;
    v_ops_sup_id UUID;
    v_dock_sup_id UUID;
    v_wh_op_id UUID;
    v_man_op_id UUID;
BEGIN
    SELECT id INTO v_ops_mgr_id FROM wms.roles WHERE name = 'OPERATIONS_MANAGER';
    SELECT id INTO v_wh_mgr_id FROM wms.roles WHERE name = 'WAREHOUSE_MANAGER';
    SELECT id INTO v_ops_sup_id FROM wms.roles WHERE name = 'OPERATIONS_SUPERVISOR';
    SELECT id INTO v_dock_sup_id FROM wms.roles WHERE name = 'DOCK_SUPERVISOR';
    SELECT id INTO v_wh_op_id FROM wms.roles WHERE name = 'WAREHOUSE_OPERATOR';
    SELECT id INTO v_man_op_id FROM wms.roles WHERE name = 'MANEUVER_OPERATOR';

    -- Reasignar usuarios de WAREHOUSE_MANAGER
    IF v_wh_mgr_id IS NOT NULL AND v_ops_mgr_id IS NOT NULL THEN
        UPDATE wms.users SET role_id = v_ops_mgr_id WHERE role_id = v_wh_mgr_id;
        DELETE FROM wms.role_permissions WHERE role_id = v_wh_mgr_id;
        DELETE FROM wms.roles WHERE id = v_wh_mgr_id;
    END IF;

    -- Reasignar usuarios de DOCK_SUPERVISOR
    IF v_dock_sup_id IS NOT NULL AND v_ops_sup_id IS NOT NULL THEN
        UPDATE wms.users SET role_id = v_ops_sup_id WHERE role_id = v_dock_sup_id;
        DELETE FROM wms.role_permissions WHERE role_id = v_dock_sup_id;
        DELETE FROM wms.roles WHERE id = v_dock_sup_id;
    END IF;

    -- Reasignar usuarios de MANEUVER_OPERATOR
    IF v_man_op_id IS NOT NULL AND v_wh_op_id IS NOT NULL THEN
        UPDATE wms.users SET role_id = v_wh_op_id WHERE role_id = v_man_op_id;
        DELETE FROM wms.role_permissions WHERE role_id = v_man_op_id;
        DELETE FROM wms.roles WHERE id = v_man_op_id;
    END IF;
END $$;
