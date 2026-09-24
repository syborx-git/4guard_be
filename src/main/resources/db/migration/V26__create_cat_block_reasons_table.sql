-- =============================================================================
-- 4GUARD WMS — Flyway Migration V26
-- Archivo: V26__create_cat_block_reasons_table.sql
-- Módulo: Catálogo de Motivos de Bloqueo de Posiciones QM (HU-048 / HU-127)
-- Descripción:
--   1. Crea tabla wms.cat_block_reasons para parametrizar causales de bloqueo.
--   2. Siembra catálogo estándar de motivos operativos, calidad y mantenimiento.
-- =============================================================================

SET search_path TO wms, public;

CREATE TABLE IF NOT EXISTS wms.cat_block_reasons (
    id          UUID PRIMARY KEY DEFAULT wms.uuid_generate_v4(),
    code        VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(200) NOT NULL,
    category    VARCHAR(50) NOT NULL,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_cat_block_reasons_active ON wms.cat_block_reasons (is_active);

-- Siembra de motivos estándar de bloqueo
INSERT INTO wms.cat_block_reasons (id, code, description, category, is_active, created_at) VALUES
    (wms.uuid_generate_v4(), 'BLOQ_CALIDAD', 'Bloqueo por Calidad / Muestreo QM', 'CALIDAD', TRUE, CURRENT_TIMESTAMP),
    (wms.uuid_generate_v4(), 'BLOQ_DANIO', 'Producto con Daño Físico o Derrame', 'OPERACION', TRUE, CURRENT_TIMESTAMP),
    (wms.uuid_generate_v4(), 'BLOQ_CUARENTENA', 'Cuarentena Sanitaria / Inspección COFEPRIS', 'NORMATIVIDAD', TRUE, CURRENT_TIMESTAMP),
    (wms.uuid_generate_v4(), 'BLOQ_INVENTARIO', 'Diferencia en Conteo Cíclico / Auditoría', 'INVENTARIO', TRUE, CURRENT_TIMESTAMP),
    (wms.uuid_generate_v4(), 'BLOQ_MANTENIMIENTO', 'Mantenimiento de Rack o Piso Dañado', 'INFRAESTRUCTURA', TRUE, CURRENT_TIMESTAMP),
    (wms.uuid_generate_v4(), 'BLOQ_SEGURIDAD', 'Riesgo de Seguridad o Desplome de Estiba', 'SEGURIDAD', TRUE, CURRENT_TIMESTAMP),
    (wms.uuid_generate_v4(), 'BLOQ_CADUCIDAD', 'Producto Próximo a Caducar / Vencido', 'CALIDAD', TRUE, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;
