-- =============================================================================
-- V62: Seed CHAT_ONLY Role and CHAT_ACCESS Permission (ADR-022)
-- Description: Permite crear usuarios con acceso exclusivo al sistema de chat
--              (Escritorio y Móvil) sin permisos operativos del WMS, y asigna
--              CHAT_ACCESS a los roles operativos existentes.
-- =============================================================================

SET search_path TO wms, public;

-- 1. INSERT SYSTEM ROLE: CHAT_ONLY
INSERT INTO wms.roles (id, name, level, is_system)
VALUES
    (uuid_generate_v4(), 'CHAT_ONLY', 5, TRUE)
ON CONFLICT (name) DO NOTHING;

-- 2. INSERT CHAT PERMISSION
INSERT INTO wms.permissions (id, name, description)
VALUES
    (uuid_generate_v4(), 'CHAT_ACCESS', 'Permite acceso al módulo de mensajería empresarial 4GUARD')
ON CONFLICT (name) DO NOTHING;

-- 3. ASSIGN CHAT_ACCESS TO CHAT_ONLY AND OPERATIONAL ROLES
INSERT INTO wms.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM wms.roles r
CROSS JOIN wms.permissions p
WHERE p.name = 'CHAT_ACCESS'
  AND r.name IN (
      'CHAT_ONLY',
      'CEO',
      'OPERATIONS_MANAGER',
      'OPERATIONS_SUPERVISOR',
      'FORKLIFT_OPERATOR',
      'SECURITY_GUARD',
      'VIGILANCIA',
      'QUALITY_INSPECTOR'
  )
ON CONFLICT (role_id, permission_id) DO NOTHING;
