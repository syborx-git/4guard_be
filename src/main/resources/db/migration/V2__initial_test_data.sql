-- =============================================================================
-- V2: Consolidated Master Seed and Reference Data
-- Author: 4GUARD Engineering Team (Senior Developer)
-- Description: Master consolidated DML seed script populating 4Guard WMS with
--              organizations, branches, system roles, 56 RBAC permissions,
--              user accounts, clients, SKUs, warehouse topology, inventory,
--              carrier catalog, supplier catalog, shifts, and alert rules.
-- =============================================================================

SET search_path TO wms, public;

-- =============================================================================
-- 1. ORGANIZATIONS AND BRANCHES
-- =============================================================================

INSERT INTO wms.organizations (id, name, code, tax_id, type, status, settings)
VALUES (
    'a53f0907-9fa5-4bdf-87db-2eb5e7683935',
    '4GUARD LOGISTICS CORP',
    '4GUARD',
    'MX-99887766-A',
    'LOGISTICS',
    'ACTIVE',
    '{"theme": "dark", "notifications": {"email": true, "telegram": false}}'::jsonb
) ON CONFLICT (code) DO NOTHING;

INSERT INTO wms.branches (id, organization_id, name, code, timezone, address_line1, status)
VALUES (
    'b73f0907-9fa5-4bdf-87db-2eb5e7683936',
    'a53f0907-9fa5-4bdf-87db-2eb5e7683935',
    'CENTRO DE DISTRIBUCION CDMX',
    'CDMX-01',
    'America/Mexico_City',
    'Av. Paseo de la Reforma 123, Ciudad de México',
    'ACTIVE'
) ON CONFLICT (organization_id, code) DO NOTHING;

-- =============================================================================
-- 2. SYSTEM ROLES
-- =============================================================================

INSERT INTO wms.roles (id, name, level, is_system)
VALUES 
    ('88888888-8888-8888-8888-888888888888', 'OPERATIONS_MANAGER', 7, TRUE),
    ('66666666-6666-6666-6666-666666666666', 'CEO',                6, TRUE),
    ('55555555-5555-5555-5555-555555555555', 'OPERATIONS_SUPERVISOR', 5, TRUE),
    ('44444444-4444-4444-4444-444444444444', 'CONTROL_DESK',       4, TRUE),
    ('33333333-3333-3333-3333-333333333333', 'SHIFT_LEADER',        3, TRUE),
    ('22222222-2222-2222-2222-222222222222', 'WAREHOUSE_OPERATOR',  2, TRUE),
    ('11111111-1111-1111-1111-111111111111', 'MANEUVER_OPERATOR',   1, TRUE)
ON CONFLICT (name) DO NOTHING;

-- =============================================================================
-- 3. PERMISSIONS CATALOG (FULL 56 PERMISSIONS)
-- =============================================================================

INSERT INTO wms.permissions (id, name, description)
VALUES 
    -- 1. inventory (5)
    (uuid_generate_v4(), 'INVENTORY_READ', 'Permite leer inventario'),
    (uuid_generate_v4(), 'INVENTORY_CREATE', 'Permite crear items de inventario'),
    (uuid_generate_v4(), 'INVENTORY_UPDATE', 'Permite actualizar items de inventario'),
    (uuid_generate_v4(), 'INVENTORY_DELETE', 'Permite borrar items de inventario'),
    (uuid_generate_v4(), 'INVENTORY_CONFIRM', 'Permite confirmar movimientos de inventario'),
    
    -- 2. receiving (4)
    (uuid_generate_v4(), 'RECEIVING_READ', 'Permite leer recepciones'),
    (uuid_generate_v4(), 'RECEIVING_CREATE', 'Permite crear recepciones'),
    (uuid_generate_v4(), 'RECEIVING_UPDATE', 'Permite actualizar recepciones'),
    (uuid_generate_v4(), 'RECEIVING_CONFIRM', 'Permite confirmar recepciones'),
    
    -- 3. picking (4)
    (uuid_generate_v4(), 'PICKING_READ', 'Permite leer órdenes de picking'),
    (uuid_generate_v4(), 'PICKING_CREATE', 'Permite crear órdenes de picking'),
    (uuid_generate_v4(), 'PICKING_UPDATE', 'Permite actualizar órdenes de picking'),
    (uuid_generate_v4(), 'PICKING_CONFIRM', 'Permite confirmar picking'),
    
    -- 4. packing (4)
    (uuid_generate_v4(), 'PACKING_READ', 'Permite leer empaque'),
    (uuid_generate_v4(), 'PACKING_CREATE', 'Permite crear empaque'),
    (uuid_generate_v4(), 'PACKING_UPDATE', 'Permite actualizar empaque'),
    (uuid_generate_v4(), 'PACKING_CONFIRM', 'Permite confirmar empaque'),
    
    -- 5. shipping (4)
    (uuid_generate_v4(), 'SHIPPING_READ', 'Permite leer embarques'),
    (uuid_generate_v4(), 'SHIPPING_CREATE', 'Permite crear embarques'),
    (uuid_generate_v4(), 'SHIPPING_UPDATE', 'Permite actualizar embarques'),
    (uuid_generate_v4(), 'SHIPPING_CONFIRM', 'Permite confirmar embarques'),
    
    -- 6. users (4)
    (uuid_generate_v4(), 'USERS_READ', 'Permite leer usuarios'),
    (uuid_generate_v4(), 'USERS_CREATE', 'Permite crear usuarios'),
    (uuid_generate_v4(), 'USERS_UPDATE', 'Permite actualizar usuarios'),
    (uuid_generate_v4(), 'USERS_DELETE', 'Permite borrar usuarios'),
    
    -- 7. clients (4)
    (uuid_generate_v4(), 'CLIENTS_READ', 'Permite leer clientes'),
    (uuid_generate_v4(), 'CLIENTS_CREATE', 'Permite crear clientes'),
    (uuid_generate_v4(), 'CLIENTS_UPDATE', 'Permite actualizar clientes'),
    (uuid_generate_v4(), 'CLIENTS_DELETE', 'Permite borrar clientes'),
    
    -- 8. dashboard (2)
    (uuid_generate_v4(), 'DASHBOARD_READ', 'Permite ver el dashboard'),
    (uuid_generate_v4(), 'DASHBOARD_EXECUTE', 'Permite ejecutar consultas de dashboard'),
    
    -- 9. layout (3)
    (uuid_generate_v4(), 'LAYOUT_READ', 'Permite leer el layout del almacén'),
    (uuid_generate_v4(), 'LAYOUT_UPDATE', 'Permite actualizar el layout del almacén'),
    (uuid_generate_v4(), 'LAYOUT_EXECUTE', 'Permite calcular optimizaciones de layout'),
    
    -- 10. quality (4)
    (uuid_generate_v4(), 'QUALITY_READ', 'Permite leer inspecciones de calidad'),
    (uuid_generate_v4(), 'QUALITY_UPDATE', 'Permite actualizar inspecciones de calidad'),
    (uuid_generate_v4(), 'QUALITY_AUTHORIZE', 'Permite autorizar calidad'),
    (uuid_generate_v4(), 'QUALITY_CONFIRM', 'Permite confirmar calidad'),
    
    -- 11. operations (4)
    (uuid_generate_v4(), 'OPERATIONS_READ', 'Permite leer bitácoras de operaciones'),
    (uuid_generate_v4(), 'OPERATIONS_CREATE', 'Permite registrar operaciones'),
    (uuid_generate_v4(), 'OPERATIONS_UPDATE', 'Permite actualizar operaciones'),
    (uuid_generate_v4(), 'OPERATIONS_EXECUTE', 'Permite ejecutar procesos operativos'),
    
    -- 12. metadata (3)
    (uuid_generate_v4(), 'METADATA_READ', 'Permite leer metadatos de transporte'),
    (uuid_generate_v4(), 'METADATA_CREATE', 'Permite registrar metadatos de transporte'),
    (uuid_generate_v4(), 'METADATA_UPDATE', 'Permite actualizar metadatos de transporte'),
    
    -- 13. ramps (4)
    (uuid_generate_v4(), 'RAMPS_READ', 'Permite leer estado de rampas'),
    (uuid_generate_v4(), 'RAMPS_CREATE', 'Permite registrar rampas'),
    (uuid_generate_v4(), 'RAMPS_UPDATE', 'Permite actualizar rampas'),
    (uuid_generate_v4(), 'RAMPS_AUTHORIZE', 'Permite autorizar asignación de rampas'),
    
    -- 14. labels (3)
    (uuid_generate_v4(), 'LABELS_READ', 'Permite leer etiquetas y códigos'),
    (uuid_generate_v4(), 'LABELS_CREATE', 'Permite generar etiquetas de código de barras'),
    (uuid_generate_v4(), 'LABELS_EXECUTE', 'Permite imprimir etiquetas'),
    
    -- 15. reports (3)
    (uuid_generate_v4(), 'REPORTS_READ', 'Permite leer reportes y estadísticas'),
    (uuid_generate_v4(), 'REPORTS_CREATE', 'Permite diseñar nuevos reportes'),
    (uuid_generate_v4(), 'REPORTS_EXECUTE', 'Permite exportar y generar reportes'),
    
    -- 16. audit (3)
    (uuid_generate_v4(), 'AUDIT_READ', 'Permite leer bitácora de auditoría'),
    (uuid_generate_v4(), 'AUDIT_CREATE', 'Permite generar registros de auditoría'),
    (uuid_generate_v4(), 'AUDIT_EXECUTE', 'Permite purgar logs de auditoría'),

    -- 17. System Administration (Roles, Permissions, Branches, Organizations, Sections, Locations)
    (uuid_generate_v4(), 'ROLES_READ', 'Permite leer roles'),
    (uuid_generate_v4(), 'ROLES_CREATE', 'Permite crear roles'),
    (uuid_generate_v4(), 'ROLES_UPDATE', 'Permite actualizar roles'),
    (uuid_generate_v4(), 'ROLES_DELETE', 'Permite borrar roles'),
    (uuid_generate_v4(), 'PERMISSIONS_READ', 'Permite leer el catálogo de permisos'),
    (uuid_generate_v4(), 'PERMISSIONS_CREATE', 'Permite crear permisos'),
    (uuid_generate_v4(), 'PERMISSIONS_DELETE', 'Permite borrar permisos'),
    (uuid_generate_v4(), 'BRANCHES_READ', 'Permite leer sucursales'),
    (uuid_generate_v4(), 'BRANCHES_CREATE', 'Permite crear sucursales'),
    (uuid_generate_v4(), 'BRANCHES_UPDATE', 'Permite actualizar sucursales'),
    (uuid_generate_v4(), 'BRANCHES_DELETE', 'Permite borrar sucursales'),
    (uuid_generate_v4(), 'ORGANIZATIONS_READ', 'Permite leer organizaciones'),
    (uuid_generate_v4(), 'ORGANIZATIONS_CREATE', 'Permite crear organizaciones'),
    (uuid_generate_v4(), 'ORGANIZATIONS_UPDATE', 'Permite actualizar organizaciones'),
    (uuid_generate_v4(), 'ORGANIZATIONS_DELETE', 'Permite borrar organizaciones'),
    (uuid_generate_v4(), 'SECTIONS_READ', 'Permite leer secciones del almacén'),
    (uuid_generate_v4(), 'SECTIONS_CREATE', 'Permite crear secciones del almacén'),
    (uuid_generate_v4(), 'SECTIONS_UPDATE', 'Permite actualizar secciones del almacén'),
    (uuid_generate_v4(), 'SECTIONS_DELETE', 'Permite borrar secciones del almacén'),
    (uuid_generate_v4(), 'LOCATIONS_READ', 'Permite leer ubicaciones del almacén'),
    (uuid_generate_v4(), 'LOCATIONS_CREATE', 'Permite crear ubicaciones del almacén'),
    (uuid_generate_v4(), 'LOCATIONS_UPDATE', 'Permite actualizar ubicaciones del almacén'),
    (uuid_generate_v4(), 'LOCATIONS_DELETE', 'Permite borrar ubicaciones del almacén'),

    -- 18. Carriers (4)
    (uuid_generate_v4(), 'CARRIERS_READ',   'Permite leer el catálogo de transportistas'),
    (uuid_generate_v4(), 'CARRIERS_CREATE', 'Permite crear transportistas en el catálogo'),
    (uuid_generate_v4(), 'CARRIERS_UPDATE', 'Permite actualizar transportistas del catálogo'),
    (uuid_generate_v4(), 'CARRIERS_DELETE', 'Permite eliminar transportistas del catálogo'),

    -- 19. Suppliers (5)
    (uuid_generate_v4(), 'SUPPLIERS_READ',          'Permite leer el catálogo de proveedores'),
    (uuid_generate_v4(), 'SUPPLIERS_CREATE',         'Permite crear proveedores en el catálogo'),
    (uuid_generate_v4(), 'SUPPLIERS_UPDATE',         'Permite actualizar proveedores del catálogo'),
    (uuid_generate_v4(), 'SUPPLIERS_DELETE',         'Permite archivar proveedores del catálogo'),
    (uuid_generate_v4(), 'SUPPLIERS_STATUS_CHANGE',  'Permite cambiar el estado operativo de un proveedor'),

    -- 20. Shifts (5)
    (uuid_generate_v4(), 'SHIFTS_READ',          'Permite ver catálogo y detalle de turnos'),
    (uuid_generate_v4(), 'SHIFTS_CREATE',        'Permite registrar nuevos turnos operativos'),
    (uuid_generate_v4(), 'SHIFTS_UPDATE',        'Permite actualizar turnos existentes'),
    (uuid_generate_v4(), 'SHIFTS_STATUS_CHANGE', 'Permite cambiar estatus ACTIVE/INACTIVE de turnos'),
    (uuid_generate_v4(), 'SHIFTS_DELETE',        'Permite realizar borrado lógico de turnos'),

    -- 21. Alerts (2)
    (uuid_generate_v4(), 'ALERTS_READ',  'Permite consultar catálogo y reglas de alertas de la organización'),
    (uuid_generate_v4(), 'ALERTS_WRITE', 'Permite registrar, editar, cambiar estatus y archivar reglas de alertas')
ON CONFLICT (name) DO NOTHING;

-- =============================================================================
-- 4. ROLE PERMISSIONS MAPPING
-- =============================================================================

-- OPERATIONS_MANAGER: Todos los permisos
INSERT INTO wms.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM wms.roles r
CROSS JOIN wms.permissions p
WHERE r.name = 'OPERATIONS_MANAGER'
ON CONFLICT DO NOTHING;

-- CEO: Lectura general
INSERT INTO wms.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM wms.roles r
JOIN wms.permissions p ON p.name LIKE '%_READ' OR p.name IN ('DASHBOARD_EXECUTE', 'REPORTS_EXECUTE')
WHERE r.name = 'CEO'
ON CONFLICT DO NOTHING;

-- OPERATIONS_SUPERVISOR: Operativo y supervisión
INSERT INTO wms.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM wms.roles r
JOIN wms.permissions p ON p.name IN (
    'INVENTORY_READ', 'RECEIVING_READ', 'PICKING_READ', 'PACKING_READ', 'SHIPPING_READ', 
    'USERS_READ', 'CLIENTS_READ', 'DASHBOARD_READ', 'LAYOUT_READ', 'QUALITY_READ', 
    'OPERATIONS_READ', 'METADATA_READ', 'RAMPS_READ', 'LABELS_READ', 'REPORTS_READ', 
    'AUDIT_READ', 'INVENTORY_UPDATE', 'RECEIVING_UPDATE', 'PICKING_UPDATE', 'PACKING_UPDATE', 
    'SHIPPING_UPDATE', 'OPERATIONS_UPDATE', 'RAMPS_UPDATE', 'METADATA_UPDATE', 'REPORTS_EXECUTE', 
    'AUDIT_CREATE', 'ROLES_READ', 'PERMISSIONS_READ', 'BRANCHES_READ', 'ORGANIZATIONS_READ',
    'SECTIONS_READ', 'SECTIONS_UPDATE', 'LOCATIONS_READ', 'LOCATIONS_UPDATE',
    'CARRIERS_READ', 'CARRIERS_UPDATE', 'SUPPLIERS_READ', 'SUPPLIERS_UPDATE', 'SUPPLIERS_STATUS_CHANGE',
    'SHIFTS_READ', 'SHIFTS_UPDATE', 'SHIFTS_STATUS_CHANGE', 'ALERTS_READ'
)
WHERE r.name = 'OPERATIONS_SUPERVISOR'
ON CONFLICT DO NOTHING;

-- CONTROL_DESK: Documentación y entradas
INSERT INTO wms.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM wms.roles r
JOIN wms.permissions p ON p.name IN (
    'INVENTORY_READ', 'RECEIVING_READ', 'SHIPPING_READ', 'METADATA_READ', 'RAMPS_READ', 
    'LAYOUT_READ', 'CLIENTS_READ', 'LABELS_READ', 'REPORTS_READ', 'OPERATIONS_READ',
    'METADATA_CREATE', 'METADATA_UPDATE', 'RECEIVING_CREATE', 'RECEIVING_UPDATE', 
    'SHIPPING_CREATE', 'SHIPPING_UPDATE', 'RAMPS_UPDATE', 'RAMPS_AUTHORIZE', 'RECEIVING_CONFIRM',
    'CARRIERS_READ', 'SUPPLIERS_READ'
)
WHERE r.name = 'CONTROL_DESK'
ON CONFLICT DO NOTHING;

-- SHIFT_LEADER: Líder de turno
INSERT INTO wms.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM wms.roles r
JOIN wms.permissions p ON p.name IN (
    'INVENTORY_READ', 'RECEIVING_READ', 'PICKING_READ', 'PACKING_READ', 'SHIPPING_READ', 
    'LAYOUT_READ', 'QUALITY_READ', 'OPERATIONS_READ', 'RAMPS_READ', 'LABELS_READ', 
    'REPORTS_READ', 'METADATA_READ', 'RAMPS_AUTHORIZE', 'RAMPS_UPDATE', 'LAYOUT_UPDATE', 
    'LAYOUT_EXECUTE', 'INVENTORY_UPDATE', 'RECEIVING_CONFIRM', 'PICKING_CONFIRM', 'PACKING_CONFIRM', 
    'SHIPPING_CONFIRM', 'QUALITY_CONFIRM', 'LABELS_CREATE', 'LABELS_EXECUTE',
    'CARRIERS_READ', 'SUPPLIERS_READ', 'SHIFTS_READ', 'SHIFTS_UPDATE', 'SHIFTS_STATUS_CHANGE'
)
WHERE r.name = 'SHIFT_LEADER'
ON CONFLICT DO NOTHING;

-- WAREHOUSE_OPERATOR: Operador
INSERT INTO wms.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM wms.roles r
JOIN wms.permissions p ON p.name IN (
    'INVENTORY_READ', 'RECEIVING_READ', 'PICKING_READ', 'PACKING_READ', 'SHIPPING_READ', 
    'LAYOUT_READ', 'LABELS_READ', 'INVENTORY_UPDATE', 'INVENTORY_CONFIRM', 'RECEIVING_UPDATE', 
    'RECEIVING_CONFIRM', 'PICKING_UPDATE', 'PICKING_CONFIRM', 'PACKING_UPDATE', 'PACKING_CONFIRM', 
    'SHIPPING_CONFIRM', 'LABELS_EXECUTE'
)
WHERE r.name = 'WAREHOUSE_OPERATOR'
ON CONFLICT DO NOTHING;

-- MANEUVER_OPERATOR: Maniobras
INSERT INTO wms.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM wms.roles r
JOIN wms.permissions p ON p.name IN (
    'INVENTORY_READ', 'RECEIVING_READ', 'PICKING_READ', 'LAYOUT_READ', 'LABELS_READ', 
    'METADATA_READ', 'PACKING_READ', 'SHIPPING_READ', 'INVENTORY_CONFIRM', 'RECEIVING_CONFIRM', 
    'PICKING_CONFIRM', 'LABELS_EXECUTE', 'PACKING_CONFIRM', 'SHIPPING_CONFIRM'
)
WHERE r.name = 'MANEUVER_OPERATOR'
ON CONFLICT DO NOTHING;

-- =============================================================================
-- 5. TEST USERS
-- Password hash: 'admin123' -> '$2a$12$C.In8jGhHR4dRJQpkyIWoeN5bLIeLh7S7rZ9azVdP26ssfuOR6Hw.'
-- =============================================================================

INSERT INTO wms.users (id, username, email, password, first_name, last_name, organization_id, branch_id, role_id, status, is_enabled, change_password_required)
VALUES 
    ('f33f0907-9fa5-4bdf-87db-2eb5e7683937', 'enrique', 'enrique@4guard.com', '$2a$12$C.In8jGhHR4dRJQpkyIWoeN5bLIeLh7S7rZ9azVdP26ssfuOR6Hw.', 'Enrique', 'Architect', 'a53f0907-9fa5-4bdf-87db-2eb5e7683935', 'b73f0907-9fa5-4bdf-87db-2eb5e7683936', '88888888-8888-8888-8888-888888888888', 'ACTIVE', TRUE, FALSE),
    ('afe4de7c-d10e-44b9-8970-46a0fda50626', 'Chris4G', 'christian@4guard.mx', '$2a$12$C.In8jGhHR4dRJQpkyIWoeN5bLIeLh7S7rZ9azVdP26ssfuOR6Hw.', 'Christian', 'Duran Garcia', 'a53f0907-9fa5-4bdf-87db-2eb5e7683935', 'b73f0907-9fa5-4bdf-87db-2eb5e7683936', '88888888-8888-8888-8888-888888888888', 'ACTIVE', TRUE, FALSE),
    ('fb31fe4c-bc27-4b1c-8846-7288812f84bf', 'Romel4G', 'romel@4guard.mx', '$2a$12$C.In8jGhHR4dRJQpkyIWoeN5bLIeLh7S7rZ9azVdP26ssfuOR6Hw.', 'Romel', 'Salgado', 'a53f0907-9fa5-4bdf-87db-2eb5e7683935', 'b73f0907-9fa5-4bdf-87db-2eb5e7683936', '88888888-8888-8888-8888-888888888888', 'ACTIVE', TRUE, FALSE)
ON CONFLICT (username) DO UPDATE SET change_password_required = FALSE;

-- =============================================================================
-- 6. CLIENTS AND SKUS
-- =============================================================================

INSERT INTO wms.clients (id, organization_id, name, external_id, tax_id, status) VALUES
    ('c73f0907-9fa5-4bdf-87db-2eb5e7683938', 'a53f0907-9fa5-4bdf-87db-2eb5e7683935', 'Nestle Test', 'NESTLE-TEST-001', 'NES920101AB1', 'ACTIVE'),
    ('c73f0907-9fa5-4bdf-87db-2eb5e7683940', 'a53f0907-9fa5-4bdf-87db-2eb5e7683935', 'Lala S.A.', 'LALA-001', 'LAL800101CD2', 'ACTIVE'),
    ('c73f0907-9fa5-4bdf-87db-2eb5e7683941', 'a53f0907-9fa5-4bdf-87db-2eb5e7683935', 'FEMSA Distribución', 'FEMSA-001', 'FEM750505EF3', 'ACTIVE'),
    ('c73f0907-9fa5-4bdf-87db-2eb5e7683942', 'a53f0907-9fa5-4bdf-87db-2eb5e7683935', 'Grupo Bimbo', 'BIMBO-001', 'BIM600303GH4', 'ACTIVE')
ON CONFLICT (id) DO NOTHING;

INSERT INTO wms.products_sku (id, client_id, code, name, description, weight, unit, created_by, updated_by) VALUES
    ('f83f0907-9fa5-4bdf-87db-2eb5e7683901', 'c73f0907-9fa5-4bdf-87db-2eb5e7683938', 'NES-NESCAFE-200G',   'Nescafé Clásico 200g',       'Café soluble instantáneo Nescafé Clásico frasco de 200g', 0.200, 'PZA',  'SYSTEM', 'SYSTEM'),
    ('f83f0907-9fa5-4bdf-87db-2eb5e7683902', 'c73f0907-9fa5-4bdf-87db-2eb5e7683938', 'NES-CARNATION-1L',    'Carnation Clavel 1L',        'Leche evaporada Carnation Clavel de 1 Litro',              1.050, 'PZA',  'SYSTEM', 'SYSTEM'),
    ('f83f0907-9fa5-4bdf-87db-2eb5e7683903', 'c73f0907-9fa5-4bdf-87db-2eb5e7683938', 'NES-ABUELITA-6P',     'Chocolate Abuelita 6 piezas', 'Chocolate para mesa Abuelita caja con 6 tablillas',        0.540, 'CAJA', 'SYSTEM', 'SYSTEM'),
    ('f83f0907-9fa5-4bdf-87db-2eb5e7683904', 'c73f0907-9fa5-4bdf-87db-2eb5e7683938', 'NES-NESQUIK-340G',    'Cereal Nesquik 340g',        'Cereal de trigo, maíz y arroz sabor a chocolate Nesquik',  0.340, 'PZA',  'SYSTEM', 'SYSTEM'),
    ('f83f0907-9fa5-4bdf-87db-2eb5e7683905', 'c73f0907-9fa5-4bdf-87db-2eb5e7683938', 'NES-STAMARIA-600ML',  'Agua Santa María 600ml',      'Agua mineral natural de manantial Santa María 600ml',      0.620, 'PZA',  'SYSTEM', 'SYSTEM')
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- 7. WAREHOUSE SECTIONS AND LOCATIONS
-- =============================================================================

INSERT INTO wms.warehouse_sections (id, branch_id, code, name, created_by, updated_by) VALUES
    ('d13f0907-9fa5-4bdf-87db-2eb5e7683911', 'b73f0907-9fa5-4bdf-87db-2eb5e7683936', 'SEC-RACK',   'Zona de Racks de Palletizado',  'SYSTEM', 'SYSTEM'),
    ('d13f0907-9fa5-4bdf-87db-2eb5e7683912', 'b73f0907-9fa5-4bdf-87db-2eb5e7683936', 'SEC-PICK',   'Área de Picking Manual',        'SYSTEM', 'SYSTEM'),
    ('d13f0907-9fa5-4bdf-87db-2eb5e7683913', 'b73f0907-9fa5-4bdf-87db-2eb5e7683936', 'SEC-RAMP',   'Andenes de Carga y Descarga',   'SYSTEM', 'SYSTEM')
ON CONFLICT (id) DO NOTHING;

INSERT INTO wms.locations (id, branch_id, section_id, code, name, zone, aisle, rack, level, position, coord_x, coord_y, coord_z, type, capacity_units, current_occupancy, is_blocked, created_by, updated_by) VALUES
    ('e13f0907-9fa5-4bdf-87db-2eb5e7683921', 'b73f0907-9fa5-4bdf-87db-2eb5e7683936', 'd13f0907-9fa5-4bdf-87db-2eb5e7683911', 'LOC-ZA-01-01-1A', 'Rack Pallet A-01-N1', 'ZA', '01', '01', 1, 'A', 5, 10, 1, 'PALLET', 1, 0, FALSE, 'SYSTEM', 'SYSTEM'),
    ('e13f0907-9fa5-4bdf-87db-2eb5e7683922', 'b73f0907-9fa5-4bdf-87db-2eb5e7683936', 'd13f0907-9fa5-4bdf-87db-2eb5e7683911', 'LOC-ZA-01-01-2A', 'Rack Pallet A-01-N2', 'ZA', '01', '01', 2, 'A', 5, 10, 2, 'PALLET', 1, 0, FALSE, 'SYSTEM', 'SYSTEM'),
    ('e13f0907-9fa5-4bdf-87db-2eb5e7683923', 'b73f0907-9fa5-4bdf-87db-2eb5e7683936', 'd13f0907-9fa5-4bdf-87db-2eb5e7683911', 'LOC-ZA-01-02-1B', 'Rack Pallet A-02-N1', 'ZA', '01', '02', 1, 'B', 8, 10, 1, 'PALLET', 1, 0, FALSE, 'SYSTEM', 'SYSTEM'),
    ('e13f0907-9fa5-4bdf-87db-2eb5e7683924', 'b73f0907-9fa5-4bdf-87db-2eb5e7683936', 'd13f0907-9fa5-4bdf-87db-2eb5e7683912', 'LOC-ZB-03-05-3C', 'Picking Manual B-05-N3', 'ZB', '03', '05', 3, 'C', 15, 20, 3, 'BIN',    1, 0, FALSE, 'SYSTEM', 'SYSTEM'),
    ('e13f0907-9fa5-4bdf-87db-2eb5e7683925', 'b73f0907-9fa5-4bdf-87db-2eb5e7683936', 'd13f0907-9fa5-4bdf-87db-2eb5e7683913', 'LOC-RAMP-01', 'Anden Rampa 1', 'ZC', '00', '00', 0, 'R1', 1, 1, 0,   'RAMP',   1, 0, FALSE, 'SYSTEM', 'SYSTEM')
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- 8. INVENTORY ITEMS
-- =============================================================================

INSERT INTO wms.inventory_items (
    id, organization_id, branch_id, client_id, sscc, external_ua, sku_id, location_id,
    state, quantity, batch_number, manufacturing_date, expiration_date, sap_folio,
    quarantine_reason, metadata, version, created_by, updated_by
) VALUES
    ('c13f0907-9fa5-4bdf-87db-2eb5e7683931', 'a53f0907-9fa5-4bdf-87db-2eb5e7683935', 'b73f0907-9fa5-4bdf-87db-2eb5e7683936', 'c73f0907-9fa5-4bdf-87db-2eb5e7683938', '375000000000000001', 'UA-001', 'f83f0907-9fa5-4bdf-87db-2eb5e7683901', 'e13f0907-9fa5-4bdf-87db-2eb5e7683921', 30, 500.000, 'B-NES-01', '2026-06-01', '2027-06-01', 'SAP-10001', NULL, '{}'::jsonb, 1, 'SYSTEM', 'SYSTEM'),
    ('c13f0907-9fa5-4bdf-87db-2eb5e7683932', 'a53f0907-9fa5-4bdf-87db-2eb5e7683935', 'b73f0907-9fa5-4bdf-87db-2eb5e7683936', 'c73f0907-9fa5-4bdf-87db-2eb5e7683938', '375000000000000002', 'UA-002', 'f83f0907-9fa5-4bdf-87db-2eb5e7683902', 'e13f0907-9fa5-4bdf-87db-2eb5e7683922', 20, 120.000, 'B-CAR-02', '2026-05-15', '2027-05-15', 'SAP-10002', 'Esperando aprobación microbiológica', '{"temperature_controlled": true}'::jsonb, 1, 'SYSTEM', 'SYSTEM'),
    ('c13f0907-9fa5-4bdf-87db-2eb5e7683933', 'a53f0907-9fa5-4bdf-87db-2eb5e7683935', 'b73f0907-9fa5-4bdf-87db-2eb5e7683936', 'c73f0907-9fa5-4bdf-87db-2eb5e7683938', '375000000000000003', 'UA-003', 'f83f0907-9fa5-4bdf-87db-2eb5e7683903', 'e13f0907-9fa5-4bdf-87db-2eb5e7683923', 30, 800.000, 'B-ABU-03', '2026-04-10', '2027-04-10', 'SAP-10003', NULL, '{}'::jsonb, 1, 'SYSTEM', 'SYSTEM'),
    ('c13f0907-9fa5-4bdf-87db-2eb5e7683934', 'a53f0907-9fa5-4bdf-87db-2eb5e7683935', 'b73f0907-9fa5-4bdf-87db-2eb5e7683936', 'c73f0907-9fa5-4bdf-87db-2eb5e7683938', '375000000000000004', 'UA-004', 'f83f0907-9fa5-4bdf-87db-2eb5e7683904', 'e13f0907-9fa5-4bdf-87db-2eb5e7683924', 30, 45.000,  'B-NESQ-04', '2026-03-20', '2027-03-20', 'SAP-10004', NULL, '{}'::jsonb, 1, 'SYSTEM', 'SYSTEM'),
    ('c13f0907-9fa5-4bdf-87db-2eb5e7683935', 'a53f0907-9fa5-4bdf-87db-2eb5e7683935', 'b73f0907-9fa5-4bdf-87db-2eb5e7683936', 'c73f0907-9fa5-4bdf-87db-2eb5e7683938', '375000000000000005', 'UA-005', 'f83f0907-9fa5-4bdf-87db-2eb5e7683905', 'e13f0907-9fa5-4bdf-87db-2eb5e7683925', 10, 2400.000, 'B-SMA-05', '2026-07-01', '2027-07-01', 'SAP-10005', NULL, '{}'::jsonb, 1, 'SYSTEM', 'SYSTEM')
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- 9. CARRIERS DATA
-- =============================================================================

INSERT INTO wms.carriers (id, organization_id, name, trade_name, tax_id, carrier_type, status, contact_name, contact_phone, contact_email, service_type, permit_number, geographic_coverage, notes) VALUES
    ('a13f0907-9fa5-4bdf-87db-2eb5e7683950', 'a53f0907-9fa5-4bdf-87db-2eb5e7683935', 'Transportes del Noreste S.A. de C.V.', 'TransNoreste', 'TN0890314AB2', 'EXTERNAL', 'ACTIVE', 'Roberto Garza Hernández', '8181234567', 'rgarza@transnoreste.com.mx', 'FTL', 'SCT-NL-00234-2022', 'Noreste, Centro y Bajío (NL, CDMX, QRO, GTO)', 'Transportista preferencial para rutas de alto volumen. Contrato vigente hasta 2027.')
ON CONFLICT (id) DO NOTHING;

INSERT INTO wms.carrier_vehicle_types (carrier_id, vehicle_type) VALUES
    ('a13f0907-9fa5-4bdf-87db-2eb5e7683950', 'CAJA_SECA'),
    ('a13f0907-9fa5-4bdf-87db-2eb5e7683950', 'PLATAFORMA'),
    ('a13f0907-9fa5-4bdf-87db-2eb5e7683950', 'TRACTOCAMION')
ON CONFLICT DO NOTHING;

INSERT INTO wms.carrier_preferred_clients (carrier_id, client_id) VALUES
    ('a13f0907-9fa5-4bdf-87db-2eb5e7683950', 'c73f0907-9fa5-4bdf-87db-2eb5e7683940'),
    ('a13f0907-9fa5-4bdf-87db-2eb5e7683950', 'c73f0907-9fa5-4bdf-87db-2eb5e7683941'),
    ('a13f0907-9fa5-4bdf-87db-2eb5e7683950', 'c73f0907-9fa5-4bdf-87db-2eb5e7683942')
ON CONFLICT DO NOTHING;

-- =============================================================================
-- 10. SUPPLIER CATALOGS AND SUPPLIERS DATA
-- =============================================================================

INSERT INTO wms.cat_supplier_types (code, label_es, label_en, is_service, sort_order) VALUES
    ('GOODS',            'Bienes y mercancías',                  'Goods',                    FALSE, 1),
    ('RAW_MATERIAL',     'Materia prima',                        'Raw Material',             FALSE, 2),
    ('PACKAGING',        'Material de empaque',                  'Packaging',                FALSE, 3),
    ('PALLETS',          'Tarimas y estibas',                    'Pallets',                  FALSE, 4),
    ('SPARE_PARTS',      'Refacciones',                          'Spare Parts',              FALSE, 5),
    ('TRANSPORT',        'Servicios de transporte (comercial)',   'Transport (commercial)',   TRUE,  6),
    ('MAINTENANCE',      'Mantenimiento',                        'Maintenance',              TRUE,  7),
    ('CLEANING',         'Limpieza industrial',                  'Cleaning',                 TRUE,  8),
    ('SECURITY',         'Seguridad y vigilancia',               'Security',                 TRUE,  9),
    ('PEST_CONTROL',     'Control de plagas / Fumigación',       'Pest Control',             TRUE,  10),
    ('TECHNOLOGY',       'Tecnología y sistemas',                'Technology',               TRUE,  11),
    ('GENERAL_SERVICES', 'Servicios generales',                  'General Services',         TRUE,  12),
    ('OTHER',            'Otros suministros',                    'Other',                    FALSE, 13)
ON CONFLICT (code) DO NOTHING;

INSERT INTO wms.cat_currencies (code, label, symbol) VALUES
    ('MXN', 'Pesos Mexicanos',  '$'),
    ('USD', 'Dólares US',       'US$'),
    ('EUR', 'Euros',            '€')
ON CONFLICT (code) DO NOTHING;

-- Suppliers 1-7
INSERT INTO wms.suppliers (id, organization_id, code, legal_name, commercial_name, tax_id, supplier_type_code, is_preferred, status, scope_type, notes, created_by, updated_by) VALUES
    ('b1000001-0000-0000-0000-000000000001', 'a53f0907-9fa5-4bdf-87db-2eb5e7683935', 'PRV-0001', 'Empaques Nacionales del Norte S.A. de C.V.', 'EmpaquesNorte', 'ENN980415HG8', 'PACKAGING', TRUE, 'ACTIVE', 'GLOBAL', 'Proveedor preferente para cajas de cartón corrugado y esquineros.', 'admin', 'jperez'),
    ('b1000001-0000-0000-0000-000000000002', 'a53f0907-9fa5-4bdf-87db-2eb5e7683935', 'PRV-0002', 'Tarimas y Tarimas del Centro S. de R.L.', 'Tarimas del Centro', 'TTC051120AB4', 'PALLETS', TRUE, 'ACTIVE', 'WAREHOUSE', 'Suministro exclusivo de tarimas CHEP y taconas tratadas con norma HT.', 'admin', 'supervisor01'),
    ('b1000001-0000-0000-0000-000000000003', 'a53f0907-9fa5-4bdf-87db-2eb5e7683935', 'PRV-0003', 'Fumigaciones y Control Ambiental Toluca S.A.', 'FumiToluca 3PL', 'FCA120803KL9', 'PEST_CONTROL', FALSE, 'ACTIVE', 'WAREHOUSE', 'Servicio mensual de fumigación y control integrado de plagas.', 'jperez', 'jperez'),
    ('b1000001-0000-0000-0000-000000000005', 'a53f0907-9fa5-4bdf-87db-2eb5e7683935', 'PRV-0005', 'Tecnología y Sistemas Logísticos MX S.A. de C.V.', 'TecnoLogística MX', 'TSL160330PQ5', 'TECHNOLOGY', TRUE, 'ACTIVE', 'GLOBAL', 'Proveedor de licencias de colectores Zebra e impresoras térmicas.', 'admin', 'admin'),
    ('b1000001-0000-0000-0000-000000000007', 'a53f0907-9fa5-4bdf-87db-2eb5e7683935', 'PRV-0007', 'Seguridad Operativa y Patrimonial del Centro S.A.', 'Seguridad Operativa', 'SOP140108ZA9', 'SECURITY', FALSE, 'ACTIVE', 'GLOBAL', 'Vigilancia 24/7 en casetas Smart Gate.', 'admin', 'supervisor01')
ON CONFLICT (organization_id, code) DO NOTHING;

INSERT INTO wms.supplier_contacts (supplier_id, full_name, job_title, email, phone, alt_phone) VALUES
    ('b1000001-0000-0000-0000-000000000001', 'Carlos Eduardo Mendoza', 'Gerente de Cuentas Clave', 'cmendoza@empaquesnorte.com.mx', '8183456789', '8181239900'),
    ('b1000001-0000-0000-0000-000000000002', 'Gabriela Silva Paredes', 'Coordinadora de Ventas', 'gsilva@tarimasdelcentro.com', '5557890123', NULL),
    ('b1000001-0000-0000-0000-000000000003', 'Ing. Rodrigo Alarcón', 'Director Operativo', 'ralarcon@fumitoluca.mx', '7229876543', NULL),
    ('b1000001-0000-0000-0000-000000000005', 'Dra. Sofía Hernández', 'Account Executive WMS/IoT', 'shernandez@tecnologistica.mx', '5511223344', NULL),
    ('b1000001-0000-0000-0000-000000000007', 'Capitán Alberto Morales', 'Comandante de Zona', 'amorales@seguridadoperativa.com.mx', '5566778899', NULL)
ON CONFLICT (supplier_id) DO NOTHING;

INSERT INTO wms.supplier_addresses (supplier_id, country, state, municipality, city, postal_code, street, exterior_number) VALUES
    ('b1000001-0000-0000-0000-000000000001', 'México', 'Nuevo León', 'Apodaca', 'Monterrey', '66600', 'Av. Industrias Alimentarias', '450'),
    ('b1000001-0000-0000-0000-000000000002', 'México', 'Estado de México', 'Toluca', 'Toluca', '50070', 'Vía José López Portillo', '1200'),
    ('b1000001-0000-0000-0000-000000000003', 'México', 'Estado de México', 'Toluca', 'Toluca', '50120', 'Av. Tecnológico', '88'),
    ('b1000001-0000-0000-0000-000000000005', 'México', 'Ciudad de México', 'Miguel Hidalgo', 'Ciudad de México', '11560', 'Av. Paseo de la Reforma', '222'),
    ('b1000001-0000-0000-0000-000000000007', 'México', 'Ciudad de México', 'Cuauhtémoc', 'Ciudad de México', '06600', 'Calle Insurgentes Sur', '105')
ON CONFLICT (supplier_id) DO NOTHING;

INSERT INTO wms.supplier_commercial_terms (supplier_id, lead_time_days, minimum_order_amount, credit_days, currency_code, quality_inspection_required) VALUES
    ('b1000001-0000-0000-0000-000000000001', 3, 15000.00, 30, 'MXN', TRUE),
    ('b1000001-0000-0000-0000-000000000002', 2, 25000.00, 45, 'MXN', TRUE),
    ('b1000001-0000-0000-0000-000000000003', 1, 5000.00, 15, 'MXN', FALSE),
    ('b1000001-0000-0000-0000-000000000005', 1, 1200.00, 30, 'USD', FALSE),
    ('b1000001-0000-0000-0000-000000000007', 1, 35000.00, 30, 'MXN', TRUE)
ON CONFLICT (supplier_id) DO NOTHING;

-- =============================================================================
-- 11. SHIFTS AND SCHEDULES DATA
-- =============================================================================

INSERT INTO wms.wms_shifts (id, code, name, description, start_time, end_time, rest_break_minutes, tolerance_minutes, is_overnight, status, scope_type, branch_id, created_by)
VALUES
    ('f13f0907-9fa5-4bdf-87db-2eb5e7683951', 'TRN-MAT-01', 'Turno Matutino CDMX',    'Jornada matutina estándar de almacén principal CDMX',    '06:00:00', '14:00:00', 30, 10, FALSE, 'ACTIVE', 'BRANCH', 'b73f0907-9fa5-4bdf-87db-2eb5e7683936', 'enrique'),
    ('f13f0907-9fa5-4bdf-87db-2eb5e7683952', 'TRN-VES-01', 'Turno Vespertino CDMX',  'Jornada vespertina para surtido y recepción vespertina', '14:00:00', '22:00:00', 45, 15, FALSE, 'ACTIVE', 'BRANCH', 'b73f0907-9fa5-4bdf-87db-2eb5e7683936', 'enrique'),
    ('f13f0907-9fa5-4bdf-87db-2eb5e7683953', 'TRN-NOC-01', 'Turno Nocturno Overnight', 'Jornada nocturna cruzando medianoche para acomodo',       '22:00:00', '06:00:00', 60, 10, TRUE,  'ACTIVE', 'BRANCH', 'b73f0907-9fa5-4bdf-87db-2eb5e7683936', 'enrique'),
    ('f13f0907-9fa5-4bdf-87db-2eb5e7683954', 'TRN-MIX-01', 'Turno Fines de Semana',   'Jornada extendida sábado y domingo para guardia',       '08:00:00', '20:00:00', 60, 15, FALSE, 'ACTIVE', 'BRANCH', 'b73f0907-9fa5-4bdf-87db-2eb5e7683936', 'enrique')
ON CONFLICT (id) DO NOTHING;

INSERT INTO wms.shift_operating_days (shift_id, day_of_week) VALUES
    ('f13f0907-9fa5-4bdf-87db-2eb5e7683951', 'MONDAY'), ('f13f0907-9fa5-4bdf-87db-2eb5e7683951', 'TUESDAY'), ('f13f0907-9fa5-4bdf-87db-2eb5e7683951', 'WEDNESDAY'), ('f13f0907-9fa5-4bdf-87db-2eb5e7683951', 'THURSDAY'), ('f13f0907-9fa5-4bdf-87db-2eb5e7683951', 'FRIDAY'), ('f13f0907-9fa5-4bdf-87db-2eb5e7683951', 'SATURDAY'),
    ('f13f0907-9fa5-4bdf-87db-2eb5e7683952', 'MONDAY'), ('f13f0907-9fa5-4bdf-87db-2eb5e7683952', 'TUESDAY'), ('f13f0907-9fa5-4bdf-87db-2eb5e7683952', 'WEDNESDAY'), ('f13f0907-9fa5-4bdf-87db-2eb5e7683952', 'THURSDAY'), ('f13f0907-9fa5-4bdf-87db-2eb5e7683952', 'FRIDAY'),
    ('f13f0907-9fa5-4bdf-87db-2eb5e7683953', 'MONDAY'), ('f13f0907-9fa5-4bdf-87db-2eb5e7683953', 'TUESDAY'), ('f13f0907-9fa5-4bdf-87db-2eb5e7683953', 'WEDNESDAY'), ('f13f0907-9fa5-4bdf-87db-2eb5e7683953', 'THURSDAY'), ('f13f0907-9fa5-4bdf-87db-2eb5e7683953', 'FRIDAY'),
    ('f13f0907-9fa5-4bdf-87db-2eb5e7683954', 'SATURDAY'), ('f13f0907-9fa5-4bdf-87db-2eb5e7683954', 'SUNDAY')
ON CONFLICT DO NOTHING;

INSERT INTO wms.user_shifts (id, user_id, shift_id, effective_start_date, created_by) VALUES
    ('a13f0907-9fa5-4bdf-87db-2eb5e7683901', 'f33f0907-9fa5-4bdf-87db-2eb5e7683937', 'f13f0907-9fa5-4bdf-87db-2eb5e7683951', '2026-01-01', 'enrique'),
    ('a13f0907-9fa5-4bdf-87db-2eb5e7683902', 'afe4de7c-d10e-44b9-8970-46a0fda50626', 'f13f0907-9fa5-4bdf-87db-2eb5e7683952', '2026-01-01', 'enrique'),
    ('a13f0907-9fa5-4bdf-87db-2eb5e7683903', 'fb31fe4c-bc27-4b1c-8846-7288812f84bf', 'f13f0907-9fa5-4bdf-87db-2eb5e7683953', '2026-01-01', 'enrique')
ON CONFLICT DO NOTHING;

-- =============================================================================
-- 12. ALERT CONFIGURATIONS AND FIRED EVENTS DATA (HU-134)
-- =============================================================================

INSERT INTO wms.alert_configurations (
    id, organization_id, name, category, event, priority, status, channels, recipients,
    condition, value, unit, recurrence, escalation, message_template, description, created_by
) VALUES
(
    'e13f0907-9fa5-4bdf-87db-2eb5e7683961',
    'a53f0907-9fa5-4bdf-87db-2eb5e7683935',
    'Tiempo Excedido en Rampa de Recepción',
    'RECEIVING',
    'WAIT_TIME_EXCEEDED',
    'HIGH',
    'ACTIVE',
    ARRAY['SYSTEM', 'PUSH'],
    ARRAY['SUPERVISOR', 'MANAGER'],
    'GREATER_THAN',
    30.00,
    'MINUTES',
    'EVERY_15_MIN',
    'AFTER_15_MIN',
    'El camión {{truck}} en la Rampa {{ramp}} ha superado los {{value}} minutos de espera.',
    'Alerta critica para evitar cuellos de botella en la descarga de andenes de entrada',
    'enrique'
),
(
    'e13f0907-9fa5-4bdf-87db-2eb5e7683962',
    'a53f0907-9fa5-4bdf-87db-2eb5e7683935',
    'Stock Bajo de Seguridad en Picking',
    'INVENTORY',
    'LOW_INVENTORY',
    'MEDIUM',
    'ACTIVE',
    ARRAY['SYSTEM'],
    ARRAY['OPERATOR', 'SUPERVISOR'],
    'LESS_THAN',
    15.00,
    'PIECES',
    'NEVER',
    'NONE',
    'El producto {{sku}} en la ubicación de picking {{location}} tiene solo {{value}} piezas disponibles.',
    'Notifica reabastecimiento urgente de mercancía desde stock de reserva a picking',
    'enrique'
),
(
    'e13f0907-9fa5-4bdf-87db-2eb5e7683963',
    'a53f0907-9fa5-4bdf-87db-2eb5e7683935',
    'Vencimiento Próximo de Lotes FEFO',
    'QUALITY',
    'LOT_EXPIRATION',
    'CRITICAL',
    'ACTIVE',
    ARRAY['SYSTEM', 'PUSH'],
    ARRAY['MANAGER', 'CLIENT'],
    'LESS_THAN',
    10.00,
    'HOURS',
    'EVERY_HOUR',
    'AFTER_30_MIN',
    'El lote {{lot}} del SKU {{sku}} vence en menos de {{value}} horas.',
    'Prioridad alta de salida para lotes caducos o promociones por fecha de caducidad',
    'enrique'
),
(
    'e13f0907-9fa5-4bdf-87db-2eb5e7683964',
    'a53f0907-9fa5-4bdf-87db-2eb5e7683935',
    'Discrepancia en Conteo Cíclico',
    'INVENTORY',
    'INVENTORY_DISCREPANCY',
    'HIGH',
    'ACTIVE',
    ARRAY['SYSTEM'],
    ARRAY['SUPERVISOR', 'ADMIN'],
    'GREATER_THAN',
    5.00,
    'PERCENTAGE',
    'NEVER',
    'AFTER_60_MIN',
    'Se detectó una variación del {{value}}% en el conteo del inventario {{location}}.',
    'Discrepancia de inventario físico vs sistema que requiere auditoría',
    'enrique'
),
(
    'e13f0907-9fa5-4bdf-87db-2eb5e7683965',
    'a53f0907-9fa5-4bdf-87db-2eb5e7683935',
    'Retraso en Salida de Embarque',
    'SHIPPING',
    'ORDER_DELAYED',
    'MEDIUM',
    'INACTIVE',
    ARRAY['SYSTEM'],
    ARRAY['SUPERVISOR'],
    'GREATER_THAN',
    60.00,
    'MINUTES',
    'NEVER',
    'NONE',
    'La orden de embarque {{order}} tiene un retraso de {{value}} minutos sobre la cita.',
    'Monitoreo de tiempos de salida de transportistas',
    'enrique'
)
ON CONFLICT (organization_id, name) DO NOTHING;

INSERT INTO wms.alert_fired_events (
    id, alert_configuration_id, organization_id, branch_id, triggered_at,
    entity_reference, evaluated_value, status, acknowledged_by, acknowledged_at
) VALUES
(
    'f13f0907-9fa5-4bdf-87db-2eb5e7683971',
    'e13f0907-9fa5-4bdf-87db-2eb5e7683961',
    'a53f0907-9fa5-4bdf-87db-2eb5e7683935',
    'b73f0907-9fa5-4bdf-87db-2eb5e7683936',
    NOW() - INTERVAL '15 minutes',
    'RAMP-04',
    42.00,
    'FIRED',
    NULL,
    NULL
),
(
    'f13f0907-9fa5-4bdf-87db-2eb5e7683972',
    'e13f0907-9fa5-4bdf-87db-2eb5e7683962',
    'a53f0907-9fa5-4bdf-87db-2eb5e7683935',
    'b73f0907-9fa5-4bdf-87db-2eb5e7683936',
    NOW() - INTERVAL '45 minutes',
    'SKU-NES-NESCAFE-200G',
    8.00,
    'ACKNOWLEDGED',
    'enrique',
    NOW() - INTERVAL '20 minutes'
),
(
    'f13f0907-9fa5-4bdf-87db-2eb5e7683973',
    'e13f0907-9fa5-4bdf-87db-2eb5e7683963',
    'a53f0907-9fa5-4bdf-87db-2eb5e7683935',
    'b73f0907-9fa5-4bdf-87db-2eb5e7683936',
    NOW() - INTERVAL '2 hours',
    'LOT-9988-CARNATION',
    6.00,
    'RESOLVED',
    'Chris4G',
    NOW() - INTERVAL '1 hour'
)
ON CONFLICT (id) DO NOTHING;
