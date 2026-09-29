-- ==============================================================================
-- 4GUARD WMS — Flyway Migration V35
-- Archivo: V35__create_quality_management_schema.sql
-- Módulo: Calidad QM (Bloqueos PNC, Liberaciones, Verificación Carga F01 y Reclamos)
-- ==============================================================================

SET search_path TO wms, public;

-- 1. Tabla de Dictámenes de Liberación
CREATE TABLE IF NOT EXISTS wms.quality_releases (
    id                      UUID PRIMARY KEY DEFAULT wms.uuid_generate_v4(),
    organization_id         UUID NOT NULL REFERENCES wms.organizations(id),
    branch_id               UUID NOT NULL REFERENCES wms.branches(id),
    folio                   VARCHAR(30) NOT NULL UNIQUE,
    incidence_id            UUID NOT NULL REFERENCES wms.incidences(id),
    item_id                 UUID NOT NULL REFERENCES wms.inventory_items(id),
    authorizer_type         VARCHAR(30) NOT NULL,
    support_type            VARCHAR(30) NOT NULL,
    support_custom_type     VARCHAR(100),
    support_subject         VARCHAR(255) NOT NULL,
    support_file_name       VARCHAR(255),
    authorized_by_name      VARCHAR(150) NOT NULL,
    authorized_by_position  VARCHAR(150) NOT NULL,
    destination             VARCHAR(30) NOT NULL,
    decision_notes          TEXT NOT NULL,
    released_by_user_id     UUID NOT NULL REFERENCES wms.users(id),
    evidence_metadata       JSONB DEFAULT '[]'::jsonb,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_qm_releases_org ON wms.quality_releases(organization_id, branch_id);
CREATE INDEX IF NOT EXISTS idx_qm_releases_item ON wms.quality_releases(item_id);
CREATE INDEX IF NOT EXISTS idx_qm_releases_incidence ON wms.quality_releases(incidence_id);

-- 2. Tabla de Verificaciones de Carga (F01-PO-GC-8.6-03 Rev. 03)
CREATE TABLE IF NOT EXISTS wms.load_verifications (
    id                      UUID PRIMARY KEY DEFAULT wms.uuid_generate_v4(),
    organization_id         UUID NOT NULL REFERENCES wms.organizations(id),
    branch_id               UUID NOT NULL REFERENCES wms.branches(id),
    folio                   VARCHAR(30) NOT NULL UNIQUE,
    control_number          VARCHAR(50) NOT NULL DEFAULT 'F01-PO-GC-8.6-03',
    revision_number         VARCHAR(10) NOT NULL DEFAULT '03',
    outbound_id             UUID REFERENCES wms.warehouse_outbounds(id),
    reception_id            UUID REFERENCES wms.warehouse_receptions(id),
    remision_number         VARCHAR(60) NOT NULL,
    product_description     VARCHAR(255) NOT NULL,
    client_name             VARCHAR(150) NOT NULL,
    verification_date       DATE NOT NULL,
    verification_time       TIME NOT NULL,
    ramp_code               VARCHAR(30) NOT NULL,
    status                  VARCHAR(30) NOT NULL,
    product_criteria        JSONB NOT NULL,
    transport_criteria      JSONB NOT NULL,
    signatures              JSONB NOT NULL,
    general_observations    TEXT,
    evidence_metadata       JSONB DEFAULT '[]'::jsonb,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_qm_verif_org ON wms.load_verifications(organization_id, branch_id);
CREATE INDEX IF NOT EXISTS idx_qm_verif_remision ON wms.load_verifications(remision_number);
CREATE INDEX IF NOT EXISTS idx_qm_verif_status ON wms.load_verifications(status);

-- 3. Extensión de wms.incidences para Soporte Integral de PNC y Reclamos F01
ALTER TABLE wms.incidences
    ADD COLUMN IF NOT EXISTS stage VARCHAR(30) DEFAULT 'STORAGE',
    ADD COLUMN IF NOT EXISTS defect_category VARCHAR(40) DEFAULT 'MATERIAL',
    ADD COLUMN IF NOT EXISTS damaged_qty NUMERIC(12,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS lost_qty NUMERIC(12,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS associated_cost NUMERIC(12,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS currency VARCHAR(10) DEFAULT 'MXN',
    ADD COLUMN IF NOT EXISTS observations TEXT,
    ADD COLUMN IF NOT EXISTS criteria_metadata JSONB DEFAULT '[]'::jsonb,
    ADD COLUMN IF NOT EXISTS evidence_metadata JSONB DEFAULT '[]'::jsonb;
