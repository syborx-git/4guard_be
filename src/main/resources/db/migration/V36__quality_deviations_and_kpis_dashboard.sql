-- ==============================================================================
-- 4GUARD WMS — Flyway Migration V36
-- Archivo: V36__quality_deviations_and_kpis_dashboard.sql
-- Módulo: Calidad QM (Captura de Desviaciones Nativas y Tablero Mensual de 10 KPIs)
-- ==============================================================================

SET search_path TO wms, public;

-- 1. Tabla Principal de Desviaciones de Calidad
CREATE TABLE IF NOT EXISTS wms.quality_deviations (
    id                          UUID PRIMARY KEY DEFAULT wms.uuid_generate_v4(),
    organization_id             UUID NOT NULL REFERENCES wms.organizations(id),
    branch_id                   UUID NOT NULL REFERENCES wms.branches(id),
    folio                       VARCHAR(30) NOT NULL UNIQUE, -- Ej. DEV-2026-0001
    remision_number             VARCHAR(100) NOT NULL,
    sku_id                      VARCHAR(50) NOT NULL,
    sku_description             VARCHAR(255),
    ua_code                     VARCHAR(50) NOT NULL,        -- SSCC de 18 dígitos o identificador de tarima
    material_type               VARCHAR(50) NOT NULL,        -- PRODUCTO_TERMINADO, EMBALAJES, CAFE_VERDE, OTRO
    deviation_date              DATE NOT NULL,
    deviation_time              TIME NOT NULL DEFAULT CURRENT_TIME,
    detected_by_id              UUID NOT NULL REFERENCES wms.users(id),
    responsible_collaborator     VARCHAR(150),
    bay_location_code           VARCHAR(50),
    damaged_units               INT NOT NULL DEFAULT 0,
    material_cost               NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    currency                    VARCHAR(10) NOT NULL DEFAULT 'MXN',
    condition_deviation         VARCHAR(100) NOT NULL,       -- PALLET_DANADO, INESTABLE, PLAGA, FRASCO_ROTO, HUMEDAD, TARIMA_MAL_ESTADO, OTRO
    root_cause_motive           VARCHAR(100) NOT NULL,       -- MANEJO_INADECUADO, INFRAESTRUCTURA, PLAGAS, LIMPIEZA, TRANSPORTE_INTERNO, EMPAQUE_ORIGINAL, OTRO
    origin_area                 VARCHAR(50) NOT NULL,        -- CALIDAD, SUPPLY, SEGURIDAD, OPERACIONES
    evidence_photo_urls         JSONB DEFAULT '[]'::jsonb,
    action_taken                VARCHAR(100) NOT NULL,       -- RECHAZO_PRODUCTO, BLOQUEO_CALIDAD, ACONDICIONAMIENTO, DEVOLUCION_PROVEEDOR, DESTRUCCION, OTRO
    observations                TEXT,
    is_resolved                 BOOLEAN NOT NULL DEFAULT FALSE,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_qm_deviations_org_branch ON wms.quality_deviations(organization_id, branch_id);
CREATE INDEX IF NOT EXISTS idx_qm_deviations_date ON wms.quality_deviations(branch_id, deviation_date);
CREATE INDEX IF NOT EXISTS idx_qm_deviations_sku ON wms.quality_deviations(sku_id);
CREATE INDEX IF NOT EXISTS idx_qm_deviations_ua ON wms.quality_deviations(ua_code);
CREATE INDEX IF NOT EXISTS idx_qm_deviations_root_cause ON wms.quality_deviations(root_cause_motive);

-- 2. Vista Materializada para Polling e Indicadores Mensuales Consolidados
CREATE MATERIALIZED VIEW IF NOT EXISTS wms.mv_quality_monthly_kpis AS
SELECT
    branch_id,
    DATE_TRUNC('month', deviation_date) AS kpi_month,
    COUNT(id) AS total_deviations,
    SUM(damaged_units) AS total_damaged_pieces,
    SUM(CASE WHEN material_type = 'PRODUCTO_TERMINADO' THEN damaged_units ELSE 0 END) AS pt_damaged_pieces,
    SUM(CASE WHEN material_type = 'EMBALAJES' THEN damaged_units ELSE 0 END) AS packaging_damaged_pieces,
    SUM(CASE WHEN material_type = 'CAFE_VERDE' THEN damaged_units ELSE 0 END) AS green_coffee_damaged_pieces,
    SUM(material_cost) AS total_non_quality_cost,
    COUNT(CASE WHEN action_taken = 'RECHAZO_PRODUCTO' THEN 1 END) AS count_rejected,
    COUNT(CASE WHEN action_taken = 'BLOQUEO_CALIDAD' THEN 1 END) AS count_blocked,
    COUNT(CASE WHEN action_taken = 'ACONDICIONAMIENTO' THEN 1 END) AS count_conditioned
FROM wms.quality_deviations
GROUP BY branch_id, DATE_TRUNC('month', deviation_date);

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_qm_monthly_kpis ON wms.mv_quality_monthly_kpis(branch_id, kpi_month);
