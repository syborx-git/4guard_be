-- ==============================================================================
-- 4GUARD WMS - FLYWAY MIGRATION V51
-- Descripcion: Segregacion de permisos de roles segun organigrama corporativo:
--              1. El Director General (CEO / ADMIN) hereda la totalidad de permisos
--                 operativos del Gerente de Operaciones + modulos de Calidad y Seguridad.
--              2. El Gerente de Operaciones (OPERATIONS_MANAGER) mantiene todos los
--                 permisos operativos del WMS pero se le revocan los accesos a los
--                 modulos de Calidad (QUALITY_*) y Caseta de Seguridad (SECURITY_GATE_*).
-- ==============================================================================

SET search_path TO wms, public;

-- 1. Asegurar que CEO herede todos los permisos de OPERATIONS_MANAGER
INSERT INTO wms.role_permissions (role_id, permission_id)
SELECT r_ceo.id, rp.permission_id
FROM wms.roles r_ceo
CROSS JOIN wms.role_permissions rp
JOIN wms.roles r_ops ON rp.role_id = r_ops.id
WHERE r_ceo.name = 'CEO' AND r_ops.name = 'OPERATIONS_MANAGER'
ON CONFLICT DO NOTHING;

-- 2. Asegurar que CEO tenga todos los permisos transversales de Calidad y Seguridad
INSERT INTO wms.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM wms.roles r
CROSS JOIN wms.permissions p
WHERE r.name = 'CEO'
  AND (p.name LIKE 'QUALITY%' OR p.name LIKE 'SECURITY_GATE%')
ON CONFLICT DO NOTHING;

-- 3. Revocar permisos de Calidad y Seguridad a OPERATIONS_MANAGER
DELETE FROM wms.role_permissions
WHERE role_id = (SELECT id FROM wms.roles WHERE name = 'OPERATIONS_MANAGER')
  AND permission_id IN (
    SELECT id FROM wms.permissions 
    WHERE name LIKE 'QUALITY%' OR name LIKE 'SECURITY_GATE%'
  );

