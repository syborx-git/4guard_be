-- =============================================================================
-- V25: Create Performance KPIs Management Table (HU-138)
-- Author: 4GUARD Engineering Team (Architect)
-- Description: Establishes wms.performance_kpis table for dynamic KPI rules,
--              thresholds, and evaluation configurations.
-- =============================================================================

SET search_path TO wms, public;

CREATE TABLE IF NOT EXISTS wms.performance_kpis (
    id                  UUID PRIMARY KEY DEFAULT wms.uuid_generate_v4(),
    organization_id     UUID REFERENCES wms.organizations(id),
    name                VARCHAR(150) NOT NULL,
    description         TEXT,
    module              VARCHAR(50) NOT NULL,
    unit                VARCHAR(50) NOT NULL,
    evaluation_type     VARCHAR(50) NOT NULL,
    target_threshold    DOUBLE PRECISION,
    warning_threshold   DOUBLE PRECISION,
    critical_threshold  DOUBLE PRECISION,
    range_low           DOUBLE PRECISION,
    range_high          DOUBLE PRECISION,
    current_value       DOUBLE PRECISION,
    last_measured_at    TIMESTAMPTZ,
    status              VARCHAR(30) DEFAULT 'OPTIMAL',
    source_process      VARCHAR(100),
    start_event         VARCHAR(100),
    end_event           VARCHAR(100),
    frequency_value     INTEGER DEFAULT 15,
    frequency_unit      VARCHAR(30) DEFAULT 'MINUTES',
    is_enabled          BOOLEAN NOT NULL DEFAULT TRUE,
    created_by_user     VARCHAR(100),
    updated_by_user     VARCHAR(100),

    -- Audit & Versioning (BaseVersionedEntity / BaseAuditEntity)
    version             BIGINT DEFAULT 0,
    created_at          TIMESTAMPTZ DEFAULT NOW(),
    updated_at          TIMESTAMPTZ DEFAULT NOW(),
    created_by          VARCHAR(36),
    updated_by          VARCHAR(36),

    CONSTRAINT uk_perf_kpi_name_module UNIQUE (organization_id, module, name)
);

CREATE INDEX IF NOT EXISTS idx_performance_kpis_module ON wms.performance_kpis(module);
CREATE INDEX IF NOT EXISTS idx_performance_kpis_enabled ON wms.performance_kpis(is_enabled);
CREATE INDEX IF NOT EXISTS idx_performance_kpis_org ON wms.performance_kpis(organization_id);

-- Seed Initial Base KPIs (HU-138)
INSERT INTO wms.performance_kpis (
    id, organization_id, name, description, module, unit, evaluation_type, 
    target_threshold, warning_threshold, critical_threshold, range_low, range_high,
    current_value, last_measured_at, status, source_process, start_event, end_event,
    frequency_value, frequency_unit, is_enabled, created_by_user, updated_by_user
) VALUES 
(
    wms.uuid_generate_v4(),
    'a53f0907-9fa5-4bdf-87db-2eb5e7683935',
    'Tiempo de descarga',
    'Tiempo promedio desde la llegada del camión hasta que se completa la descarga total de mercancía.',
    'RECEIVING',
    'MINUTES',
    'LOWER_IS_BETTER',
    45.0, 60.0, 90.0, NULL, NULL,
    38.0, NOW(), 'OPTIMAL', 'Recepción', 'Llegada del camión', 'Fin de descarga',
    5, 'MINUTES', TRUE, 'SYSTEM', 'SYSTEM'
),
(
    wms.uuid_generate_v4(),
    'a53f0907-9fa5-4bdf-87db-2eb5e7683935',
    'Exactitud de inventario',
    'Porcentaje de coincidencia entre el inventario físico y el registrado en el sistema WMS.',
    'INVENTORY',
    'PERCENTAGE',
    'HIGHER_IS_BETTER',
    99.0, 95.0, 90.0, NULL, NULL,
    96.2, NOW(), 'WARNING', 'Inventario cíclico', 'Inicio de conteo cíclico', 'Cierre de conteo cíclico',
    1, 'HOURS', TRUE, 'SYSTEM', 'SYSTEM'
),
(
    wms.uuid_generate_v4(),
    'a53f0907-9fa5-4bdf-87db-2eb5e7683935',
    'Ocupación del almacén',
    'Porcentaje de ubicaciones ocupadas respecto al total de ubicaciones disponibles.',
    'INVENTORY',
    'PERCENTAGE',
    'RANGE',
    0.0, 10.0, 20.0, 60.0, 85.0,
    72.0, NOW(), 'OPTIMAL', 'Gestión de ubicaciones', 'Cálculo de ocupación', 'Reporte de ocupación',
    30, 'MINUTES', TRUE, 'SYSTEM', 'SYSTEM'
),
(
    wms.uuid_generate_v4(),
    'a53f0907-9fa5-4bdf-87db-2eb5e7683935',
    'Productividad de picking',
    'Cantidad de unidades o líneas procesadas por hora por operador.',
    'PICKING',
    'UNITS_PER_HOUR',
    'HIGHER_IS_BETTER',
    120.0, 90.0, 60.0, NULL, NULL,
    115.0, NOW(), 'OPTIMAL', 'Picking', 'Asignación de tarea de picking', 'Confirmación de picking completo',
    15, 'MINUTES', TRUE, 'SYSTEM', 'SYSTEM'
),
(
    wms.uuid_generate_v4(),
    'a53f0907-9fa5-4bdf-87db-2eb5e7683935',
    'Tiempo de embarque',
    'Tiempo promedio desde el inicio de la carga del camión hasta el cierre del embarque.',
    'SHIPPING',
    'MINUTES',
    'LOWER_IS_BETTER',
    30.0, 50.0, 75.0, NULL, NULL,
    82.0, NOW(), 'CRITICAL', 'Embarques', 'Inicio de carga', 'Cierre de embarque',
    10, 'MINUTES', TRUE, 'SYSTEM', 'SYSTEM'
),
(
    wms.uuid_generate_v4(),
    'a53f0907-9fa5-4bdf-87db-2eb5e7683935',
    'Puntualidad de transportistas',
    'Porcentaje de transportistas que llegan dentro de la ventana horaria programada.',
    'CARRIERS',
    'PERCENTAGE',
    'HIGHER_IS_BETTER',
    95.0, 85.0, 70.0, NULL, NULL,
    83.0, NOW(), 'WARNING', 'Control de citas', 'Hora programada de cita', 'Check-in real del transportista',
    1, 'HOURS', TRUE, 'SYSTEM', 'SYSTEM'
)
ON CONFLICT DO NOTHING;
