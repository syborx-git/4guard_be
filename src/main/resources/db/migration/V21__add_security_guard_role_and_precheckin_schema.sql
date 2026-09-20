-- =============================================================================
-- V21: Add Security Guard Role, Permissions, and Pre-CheckIn Digital Passes Schema
-- Author: 4GUARD Engineering Team (Architect)
-- Description: Establishes dedicated RBAC role and permissions for Security Gate (Caseta),
--              creates wms.security_pre_checkins table for QR driver self-registration (F01-PO-CP-7.1.3-03),
--              and seeds default security guard account.
-- =============================================================================

SET search_path TO wms, public;

-- 1. INSERT SYSTEM ROLE: SECURITY_GUARD / VIGILANCIA
INSERT INTO wms.roles (id, name, level, is_system)
VALUES 
    ('77777777-7777-7777-7777-777777777777', 'SECURITY_GUARD', 2, TRUE),
    ('77777777-7777-7777-7777-777777777778', 'VIGILANCIA',     2, TRUE)
ON CONFLICT (name) DO NOTHING;

-- 2. INSERT SECURITY GATE PERMISSIONS
INSERT INTO wms.permissions (id, name, description)
VALUES 
    (uuid_generate_v4(), 'SECURITY_GATE_READ',   'Permite consultar pases y registros de caseta de seguridad'),
    (uuid_generate_v4(), 'SECURITY_GATE_CREATE', 'Permite generar pases QR y registrar entradas de transporte'),
    (uuid_generate_v4(), 'SECURITY_GATE_UPDATE', 'Permite verificar, editar y autorizar acceso en caseta')
ON CONFLICT (name) DO NOTHING;

-- 3. ASSIGN PERMISSIONS TO ROLES
INSERT INTO wms.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM wms.roles r
CROSS JOIN wms.permissions p
WHERE r.name IN ('SECURITY_GUARD', 'VIGILANCIA')
  AND p.name IN (
      'SECURITY_GATE_READ',
      'SECURITY_GATE_CREATE',
      'SECURITY_GATE_UPDATE',
      'CARRIERS_READ',
      'CLIENTS_READ',
      'WAREHOUSE_MOVEMENTS_READ',
      'WAREHOUSE_MOVEMENTS_CREATE'
  )
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Assign security permissions to Administrators and Supervisors
INSERT INTO wms.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM wms.roles r
CROSS JOIN wms.permissions p
WHERE r.name IN ('OPERATIONS_MANAGER', 'OPERATIONS_SUPERVISOR', 'CEO')
  AND p.name IN ('SECURITY_GATE_READ', 'SECURITY_GATE_CREATE', 'SECURITY_GATE_UPDATE')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- 4. SEED DEFAULT SECURITY GUARD USER
INSERT INTO wms.users (
    id, username, email, password, first_name, last_name, 
    organization_id, branch_id, role_id, status, is_enabled, change_password_required
)
VALUES (
    'c11f0907-9fa5-4bdf-87db-2eb5e7683999',
    'guardia',
    'guardia@4guard.com',
    '$2a$12$C.In8jGhHR4dRJQpkyIWoeN5bLIeLh7S7rZ9azVdP26ssfuOR6Hw.',
    'Oficial de Turno',
    'Caseta Principal',
    'a53f0907-9fa5-4bdf-87db-2eb5e7683935',
    'b73f0907-9fa5-4bdf-87db-2eb5e7683936',
    '77777777-7777-7777-7777-777777777777',
    'ACTIVE',
    TRUE,
    FALSE
)
ON CONFLICT (username) DO UPDATE SET 
    role_id = '77777777-7777-7777-7777-777777777777',
    is_enabled = TRUE,
    change_password_required = FALSE;

-- 5. CREATE SECURITY PRE-CHECKINS TABLE (QR DIGITAL PASSES & DRIVER SELF-REGISTRATION)
CREATE TABLE IF NOT EXISTS wms.security_pre_checkins (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    token                   VARCHAR(64) UNIQUE NOT NULL,
    organization_id         UUID NOT NULL REFERENCES wms.organizations(id),
    branch_id               UUID NOT NULL REFERENCES wms.branches(id),
    status                  VARCHAR(30) NOT NULL DEFAULT 'PENDING_DRIVER', -- PENDING_DRIVER, SUBMITTED, COMPLETED, CANCELLED
    operation_type          VARCHAR(20) NOT NULL DEFAULT 'DESCARGA',       -- CARGA, DESCARGA
    
    -- Client & Carrier
    client_id               UUID REFERENCES wms.clients(id),
    client_code             VARCHAR(100),
    client_name             VARCHAR(200),
    carrier_id              UUID REFERENCES wms.carriers(id),
    carrier_line_code       VARCHAR(100),
    carrier_line            VARCHAR(200),
    
    -- Transport & Driver
    driver_name             VARCHAR(200),
    driver_license          VARCHAR(100),
    transport_type          VARCHAR(100),
    economic_number         VARCHAR(100),
    box_economic_number     VARCHAR(100),
    tractor_plates          VARCHAR(50),
    box_plates              VARCHAR(50),
    box_dimensions          VARCHAR(50),
    seal_numbers            TEXT[],
    
    -- Documents
    doc_number              VARCHAR(100),
    doc_date                DATE,
    reception_time          TIME,
    departure_time          TIME,
    
    -- Checklist & Inspection (F01-PO-CP-7.1.3-03)
    checklist_data          JSONB,
    observations            TEXT,
    driver_signature        TEXT,
    driver_signed_at        TIMESTAMPTZ,
    
    -- Resolution by Security Guard
    ramp_id                 UUID REFERENCES wms.locations(id),
    ramp_number             INTEGER,
    ramp_code               VARCHAR(50),
    forklift_operator_id    UUID REFERENCES wms.forklift_operators(id),
    forklift_operator_name  VARCHAR(150),
    guard_notes             TEXT,
    generated_folio         VARCHAR(50),
    processed_by            VARCHAR(100),
    processed_at            TIMESTAMPTZ,
    
    expires_at              TIMESTAMPTZ NOT NULL,
    created_at              TIMESTAMPTZ DEFAULT NOW(),
    updated_at              TIMESTAMPTZ DEFAULT NOW(),
    created_by              VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_security_precheckins_token ON wms.security_pre_checkins(token);
CREATE INDEX IF NOT EXISTS idx_security_precheckins_status ON wms.security_pre_checkins(status);
CREATE INDEX IF NOT EXISTS idx_security_precheckins_org_branch ON wms.security_pre_checkins(organization_id, branch_id);
