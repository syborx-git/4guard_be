-- ==============================================================================
-- 4GUARD WMS - FLYWAY MIGRATION V57
-- Descripcion: Alineacion integral de permisos RBAC para los perfiles operativos:
--              1. Inserta permisos granulares faltantes en wms.permissions.
--              2. Asegura que OPERATIONS_MANAGER, CEO y ADMIN tengan cobertura
--                 completa en Recepcion, Seguridad, Movimientos y Calidad.
--              3. Configura CONTROL_DESK (Mesa de Control) con exactamente los
--                 mismos permisos que OPERATIONS_MANAGER, excluyendo Rendimiento
--                 (PERFORMANCE_*, REPORTS_*) y Administrar (USERS_*, ROLES_*, PERMISSIONS_*).
--              4. Refuerza permisos de soporte para SECURITY_GUARD y WAREHOUSE_OPERATOR.
-- ==============================================================================

SET search_path TO wms, public;

-- 1. INSERTAR PERMISOS GRANULARES USADOS EN @PreAuthorize QUE NO EXISTIAN EN BD
INSERT INTO wms.permissions (id, name, description)
VALUES
    (uuid_generate_v4(), 'WAREHOUSE_MOVEMENTS_READ',      'Permite consultar recepciones, traspasos y salidas de almacen'),
    (uuid_generate_v4(), 'WAREHOUSE_MOVEMENTS_CREATE',    'Permite registrar entradas, traspasos y salidas de almacen'),
    (uuid_generate_v4(), 'WAREHOUSE_MOVEMENTS_UPDATE',    'Permite actualizar y operar maniobras de almacen y terminal RF'),
    (uuid_generate_v4(), 'WAREHOUSE_MOVEMENTS_AUTHORIZE', 'Permite autorizar cierres y cambios en movimientos de almacen'),
    (uuid_generate_v4(), 'WAREHOUSE_MOVEMENTS_CANCEL',    'Permite cancelar recepciones, traspasos y salidas de almacen'),
    (uuid_generate_v4(), 'SECURITY_GATE_DELETE',          'Permite descartar o cancelar pases de acceso en caseta de seguridad'),
    (uuid_generate_v4(), 'RECEIVING_WRITE',               'Permite escribir y modificar datos operativos de recepcion'),
    (uuid_generate_v4(), 'SHIPPING_WRITE',                'Permite escribir y modificar datos operativos de embarques/salidas'),
    (uuid_generate_v4(), 'OUTBOUND_READ',                 'Permite consultar despachos y salidas de almacen'),
    (uuid_generate_v4(), 'QUALITY_WRITE',                 'Permite registrar retenciones, bloqueos y dictamenes de calidad')
ON CONFLICT (name) DO NOTHING;

-- 2. ASEGURAR COBERTURA COMPLETA PARA OPERATIONS_MANAGER, CEO Y ADMIN
INSERT INTO wms.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM wms.roles r
CROSS JOIN wms.permissions p
WHERE r.name IN ('OPERATIONS_MANAGER', 'CEO', 'ADMIN', 'SUPER_ADMIN')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 3. CONFIGURAR MESA DE CONTROL (CONTROL_DESK):
--    Mismos permisos que OPERATIONS_MANAGER EXCEPTO Rendimiento y Administrar Usuarios/Roles
DELETE FROM wms.role_permissions
WHERE role_id IN (SELECT id FROM wms.roles WHERE name = 'CONTROL_DESK')
  AND permission_id IN (
      SELECT id FROM wms.permissions
      WHERE name LIKE 'USERS_%'
         OR name LIKE 'ROLES_%'
         OR name LIKE 'PERMISSIONS_%'
         OR name LIKE 'PERFORMANCE_%'
         OR name LIKE 'REPORTS_%'
  );

INSERT INTO wms.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM wms.roles r
CROSS JOIN wms.permissions p
WHERE r.name = 'CONTROL_DESK'
  AND p.name NOT LIKE 'USERS_%'
  AND p.name NOT LIKE 'ROLES_%'
  AND p.name NOT LIKE 'PERMISSIONS_%'
  AND p.name NOT LIKE 'PERFORMANCE_%'
  AND p.name NOT LIKE 'REPORTS_%'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 4. REFORZAR PERMISOS OPERATIVOS PARA GUARDIA DE SEGURIDAD (SECURITY_GUARD / VIGILANCIA)
INSERT INTO wms.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM wms.roles r
CROSS JOIN wms.permissions p
WHERE r.name IN ('SECURITY_GUARD', 'VIGILANCIA')
  AND p.name IN (
      'SECURITY_GATE_READ',
      'SECURITY_GATE_CREATE',
      'SECURITY_GATE_UPDATE',
      'SECURITY_GATE_DELETE',
      'WAREHOUSE_MOVEMENTS_READ',
      'WAREHOUSE_MOVEMENTS_CREATE',
      'RECEIVING_READ',
      'RECEIVING_CREATE',
      'SHIPPING_READ',
      'OUTBOUND_READ',
      'INVENTORY_READ',
      'CLIENTS_READ',
      'CARRIERS_READ',
      'SUPPLIERS_READ',
      'FORKLIFT_OPERATORS_READ',
      'BRANCHES_READ',
      'ORGANIZATIONS_READ',
      'SHIFTS_READ',
      'SECTIONS_READ',
      'LOCATIONS_READ'
  )
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 5. REFORZAR PERMISOS OPERATIVOS PARA MONTACARGUISTA (WAREHOUSE_OPERATOR)
INSERT INTO wms.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM wms.roles r
CROSS JOIN wms.permissions p
WHERE r.name = 'WAREHOUSE_OPERATOR'
  AND p.name IN (
      'WAREHOUSE_MOVEMENTS_READ',
      'WAREHOUSE_MOVEMENTS_CREATE',
      'WAREHOUSE_MOVEMENTS_UPDATE',
      'RECEIVING_READ',
      'RECEIVING_UPDATE',
      'RECEIVING_CONFIRM',
      'RECEIVING_WRITE',
      'SHIPPING_READ',
      'SHIPPING_UPDATE',
      'SHIPPING_CONFIRM',
      'SHIPPING_WRITE',
      'OUTBOUND_READ',
      'PICKING_READ',
      'PICKING_UPDATE',
      'PICKING_CONFIRM',
      'PACKING_READ',
      'PACKING_UPDATE',
      'PACKING_CONFIRM',
      'INVENTORY_READ',
      'INVENTORY_UPDATE',
      'INVENTORY_CONFIRM',
      'LAYOUT_READ',
      'SECTIONS_READ',
      'LOCATIONS_READ',
      'LOCATIONS_UPDATE',
      'LABELS_READ',
      'LABELS_EXECUTE',
      'QUALITY_READ',
      'QUALITY_UPDATE',
      'QUALITY_WRITE',
      'CLIENTS_READ',
      'CARRIERS_READ',
      'SUPPLIERS_READ',
      'FORKLIFT_OPERATORS_READ',
      'SHIFTS_READ',
      'BRANCHES_READ',
      'ORGANIZATIONS_READ'
  )
ON CONFLICT (role_id, permission_id) DO NOTHING;

