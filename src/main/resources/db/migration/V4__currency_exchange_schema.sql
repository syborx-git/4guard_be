-- =============================================================================
-- V4: Currency Exchange Module Schema and Initial Seed Data
-- Author: 4GUARD Engineering Team
-- Description: Creates wms.currencies, wms.exchange_rates, and wms.currency_exchange_audit
--              tables, indexes, foreign keys, and seed data for demo organization.
-- =============================================================================

SET search_path TO wms, public;

-- 1. Tabla de Catálogo de Monedas / Divisas por Organización (Multi-Tenant)
CREATE TABLE wms.currencies (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id     UUID NOT NULL REFERENCES wms.organizations(id) ON DELETE RESTRICT,
    code                VARCHAR(3) NOT NULL,
    name                VARCHAR(100) NOT NULL,
    symbol              VARCHAR(10) NOT NULL,
    is_base             BOOLEAN NOT NULL DEFAULT FALSE,
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    decimal_places      INT NOT NULL DEFAULT 2 CHECK (decimal_places BETWEEN 0 AND 8),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(100),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by          VARCHAR(100),

    CONSTRAINT uk_currencies_org_code UNIQUE (organization_id, code)
);

CREATE INDEX idx_currencies_org ON wms.currencies(organization_id);
CREATE INDEX idx_currencies_org_code ON wms.currencies(organization_id, code);
CREATE INDEX idx_currencies_org_is_base ON wms.currencies(organization_id, is_base);

-- 2. Tabla de Histórico y Vigencia de Tipos de Cambio
CREATE TABLE wms.exchange_rates (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id     UUID NOT NULL REFERENCES wms.organizations(id) ON DELETE RESTRICT,
    from_currency_id    UUID NOT NULL REFERENCES wms.currencies(id) ON DELETE RESTRICT,
    to_currency_id      UUID NOT NULL REFERENCES wms.currencies(id) ON DELETE RESTRICT,
    rate                NUMERIC(18, 6) NOT NULL CHECK (rate > 0),
    inverse_rate        NUMERIC(18, 6) NOT NULL CHECK (inverse_rate > 0),
    effective_date      DATE NOT NULL,
    source_type         VARCHAR(20) NOT NULL DEFAULT 'MANUAL' CHECK (source_type IN ('MANUAL', 'CENTRAL_BANK', 'API_AUTO', 'CUSTOM')),
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'HISTORICAL', 'SUPERSEDED')),
    notes               VARCHAR(255),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(100),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by          VARCHAR(100)
);

CREATE INDEX idx_exchange_rates_org ON wms.exchange_rates(organization_id);
CREATE INDEX idx_exchange_rates_lookup ON wms.exchange_rates(organization_id, from_currency_id, to_currency_id, effective_date, status);

-- 3. Tabla de Bitácora de Auditoría del Módulo de Tipos de Cambio
CREATE TABLE wms.currency_exchange_audit (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id     UUID NOT NULL REFERENCES wms.organizations(id) ON DELETE RESTRICT,
    entity_type         VARCHAR(50) NOT NULL CHECK (entity_type IN ('CURRENCY', 'EXCHANGE_RATE')),
    entity_id           UUID NOT NULL,
    action              VARCHAR(50) NOT NULL CHECK (action IN ('CREATED', 'UPDATED', 'SET_BASE', 'RATE_CHANGED', 'STATUS_CHANGED')),
    description         TEXT NOT NULL,
    previous_value      JSONB,
    new_value           JSONB,
    performed_by        VARCHAR(100) NOT NULL,
    performed_at        TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_curr_audit_org ON wms.currency_exchange_audit(organization_id);
CREATE INDEX idx_curr_audit_entity ON wms.currency_exchange_audit(entity_type, entity_id);
CREATE INDEX idx_curr_audit_action_date ON wms.currency_exchange_audit(action, performed_at);

-- 4. Initial Seed Data para Organización Demo 'a53f0907-9fa5-4bdf-87db-2eb5e7683935'
INSERT INTO wms.currencies (
    id, organization_id, code, name, symbol, is_base, status, decimal_places, created_by, updated_by
) VALUES 
(
    'c13f0907-9fa5-4bdf-87db-2eb5e7683901',
    'a53f0907-9fa5-4bdf-87db-2eb5e7683935',
    'USD',
    'Dólar Estadounidense',
    '$',
    TRUE,
    'ACTIVE',
    2,
    'SYSTEM',
    'SYSTEM'
),
(
    'c13f0907-9fa5-4bdf-87db-2eb5e7683902',
    'a53f0907-9fa5-4bdf-87db-2eb5e7683935',
    'MXN',
    'Peso Mexicano',
    '$',
    FALSE,
    'ACTIVE',
    2,
    'SYSTEM',
    'SYSTEM'
),
(
    'c13f0907-9fa5-4bdf-87db-2eb5e7683903',
    'a53f0907-9fa5-4bdf-87db-2eb5e7683935',
    'EUR',
    'Euro',
    '€',
    FALSE,
    'ACTIVE',
    2,
    'SYSTEM',
    'SYSTEM'
)
ON CONFLICT (id) DO NOTHING;

-- Seed Exchange Rates
INSERT INTO wms.exchange_rates (
    id, organization_id, from_currency_id, to_currency_id, rate, inverse_rate, effective_date, source_type, status, notes, created_by, updated_by
) VALUES
(
    'e13f0907-9fa5-4bdf-87db-2eb5e7683901',
    'a53f0907-9fa5-4bdf-87db-2eb5e7683935',
    'c13f0907-9fa5-4bdf-87db-2eb5e7683901', -- USD
    'c13f0907-9fa5-4bdf-87db-2eb5e7683902', -- MXN
    18.450000,
    0.054201,
    CURRENT_DATE,
    'MANUAL',
    'ACTIVE',
    'Tipo de cambio inicial USD a MXN',
    'SYSTEM',
    'SYSTEM'
),
(
    'e13f0907-9fa5-4bdf-87db-2eb5e7683902',
    'a53f0907-9fa5-4bdf-87db-2eb5e7683935',
    'c13f0907-9fa5-4bdf-87db-2eb5e7683901', -- USD
    'c13f0907-9fa5-4bdf-87db-2eb5e7683903', -- EUR
    0.920000,
    1.086957,
    CURRENT_DATE,
    'MANUAL',
    'ACTIVE',
    'Tipo de cambio inicial USD a EUR',
    'SYSTEM',
    'SYSTEM'
)
ON CONFLICT (id) DO NOTHING;

-- Seed Audit Log Entry
INSERT INTO wms.currency_exchange_audit (
    id, organization_id, entity_type, entity_id, action, description, previous_value, new_value, performed_by
) VALUES
(
    'a13f0907-9fa5-4bdf-87db-2eb5e7683901',
    'a53f0907-9fa5-4bdf-87db-2eb5e7683935',
    'CURRENCY',
    'c13f0907-9fa5-4bdf-87db-2eb5e7683901',
    'CREATED',
    'Creación e inicialización de divisa base USD',
    NULL,
    '{"code": "USD", "name": "Dólar Estadounidense", "is_base": true}'::jsonb,
    'SYSTEM'
)
ON CONFLICT (id) DO NOTHING;

-- 5. Configurar cuentas demo para desactivar requerimiento de cambio de contraseña obligatoria
UPDATE wms.users 
SET change_password_required = FALSE,
    failed_attempts = 0,
    locked_until = NULL,
    permanently_locked = FALSE
WHERE username IN ('enrique', 'Chris4G', 'Romel4G');
