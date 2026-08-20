-- =============================================================================
-- V3: WMS Licenses Schema and Initial Seed Data
-- Author: 4GUARD Engineering Team (Senior Backend Engineer)
-- Description: Creates wms_licenses table, wms_license_history audit log table,
--              v_license_usage view, indexes, and populates demo license data.
-- =============================================================================

SET search_path TO wms, public;

-- 1. Tabla Principal de Licencias WMS
CREATE TABLE wms.wms_licenses (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id         UUID NOT NULL REFERENCES wms.organizations(id),
    organization_name       VARCHAR(150) NOT NULL,
    license_name            VARCHAR(150) NOT NULL,
    license_key_hash        VARCHAR(255) NOT NULL,
    masked_license_key      VARCHAR(50)  NOT NULL,
    plan                    VARCHAR(30)  NOT NULL CHECK (plan IN ('STARTER', 'PROFESSIONAL', 'ENTERPRISE', 'CUSTOM')),
    description             TEXT,
    valid_from              TIMESTAMPTZ  NOT NULL,
    valid_until             TIMESTAMPTZ  NOT NULL,
    grace_period_days       INT          DEFAULT 15 CHECK (grace_period_days BETWEEN 0 AND 90),
    auto_renewal            BOOLEAN      DEFAULT FALSE,
    admin_status            VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' CHECK (admin_status IN ('DRAFT', 'ACTIVE', 'SUSPENDED', 'REVOKED')),
    
    -- Capacidades contratadas (Límites)
    max_users               INT          NOT NULL DEFAULT 10,
    max_concurrent_users    INT          NOT NULL DEFAULT 5,
    max_warehouses          INT          NOT NULL DEFAULT 1,
    max_handheld_devices    INT          NOT NULL DEFAULT 5,
    max_integrations        INT          NOT NULL DEFAULT 1,
    
    -- Módulos contratados
    enabled_modules         JSONB        NOT NULL DEFAULT '["WMS_CORE"]'::jsonb,
    
    administrative_reason   TEXT,
    observations            TEXT,
    created_at              TIMESTAMPTZ  DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMPTZ  DEFAULT CURRENT_TIMESTAMP,
    updated_by              VARCHAR(100) NOT NULL DEFAULT 'SYSTEM'
);

CREATE INDEX idx_wms_licenses_org ON wms.wms_licenses(organization_id);
CREATE INDEX idx_wms_licenses_status ON wms.wms_licenses(admin_status);

-- 2. Tabla de Historial y Bitácora Forense de Licencias
CREATE TABLE wms.wms_license_history (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    license_id              UUID NOT NULL REFERENCES wms.wms_licenses(id) ON DELETE CASCADE,
    action                  VARCHAR(40) NOT NULL CHECK (action IN (
        'CREATED', 'UPDATED', 'RENEWED', 'SUSPENDED', 'REACTIVATED', 'REVOKED',
        'CAPACITY_CHANGED', 'MODULES_CHANGED', 'KEY_REGENERATED'
    )),
    description             TEXT NOT NULL,
    previous_value          JSONB,
    new_value               JSONB,
    performed_by            VARCHAR(100) NOT NULL,
    performed_at            TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_license_history_lic ON wms.wms_license_history(license_id);

-- 3. Vista de Métricas de Uso de Licencia
CREATE VIEW wms.v_license_usage AS
SELECT 
    l.id AS license_id,
    (SELECT COUNT(*) FROM wms.users u WHERE u.organization_id = l.organization_id AND u.status = 'ACTIVE') AS current_users,
    0::BIGINT AS concurrent_users_peak,
    (SELECT COUNT(*) FROM wms.branches b WHERE b.organization_id = l.organization_id AND b.status = 'ACTIVE') AS current_warehouses,
    0::BIGINT AS registered_handheld_devices,
    0::BIGINT AS active_integrations
FROM wms.wms_licenses l;

-- 4. Initial Seed Data
INSERT INTO wms.wms_licenses (
    id, organization_id, organization_name, license_name, license_key_hash, masked_license_key,
    plan, description, valid_from, valid_until, grace_period_days, auto_renewal, admin_status,
    max_users, max_concurrent_users, max_warehouses, max_handheld_devices, max_integrations,
    enabled_modules, administrative_reason, observations, updated_by
) VALUES (
    'e13f0907-9fa5-4bdf-87db-2eb5e7683990',
    'a53f0907-9fa5-4bdf-87db-2eb5e7683935',
    '4GUARD LOGISTICS CORP',
    'Licencia Enterprise 4GUARD Corporate',
    '$2a$12$C.In8jGhHR4dRJQpkyIWoeN5bLIeLh7S7rZ9azVdP26ssfuOR6Hw.',
    '4GD-ENT-••••-••••-9X21',
    'ENTERPRISE',
    'Licencia corporativa ilimitada para la sede principal de 4Guard WMS',
    NOW() - INTERVAL '30 days',
    NOW() + INTERVAL '335 days',
    15,
    TRUE,
    'ACTIVE',
    50,
    25,
    5,
    20,
    10,
    '["WMS_CORE", "INVENTORY", "QUALITY", "CARRIERS", "SUPPLIERS", "SHIFTS", "ALERTS"]'::jsonb,
    'Emisión inicial por contratación anual',
    'Cliente VIP Corporativo',
    'SYSTEM'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO wms.wms_license_history (
    id, license_id, action, description, previous_value, new_value, performed_by
) VALUES (
    'f13f0907-9fa5-4bdf-87db-2eb5e7683991',
    'e13f0907-9fa5-4bdf-87db-2eb5e7683990',
    'CREATED',
    'Emisión inicial de licencia Enterprise corporativa',
    NULL,
    '{"plan": "ENTERPRISE", "max_users": 50, "admin_status": "ACTIVE"}'::jsonb,
    'enrique'
) ON CONFLICT (id) DO NOTHING;
