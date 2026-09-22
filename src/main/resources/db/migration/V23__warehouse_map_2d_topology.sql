-- =============================================================================
-- 4GUARD WMS — Flyway Migration V23
-- Archivo: V23__warehouse_map_2d_topology.sql
-- Módulo: Topología Física 2D y Mapa Interactivo de Almacén (HU-048 / HU-127)
-- Descripción:
--   1. Agrega metadatos espaciales y de estiba a wms.warehouse_sections.
--   2. Registra la sección 'SEC-ALM-K' y normaliza capacidades de naves A-L.
--   3. Crea tabla de catálogo para motivos de bloqueo QM (wms.cat_block_reasons).
--   4. Siembra las 885 posiciones operativas en wms.locations para la sucursal activa.
--   5. Crea tabla de vinculación de SKUs por sección (wms.warehouse_section_skus).
--   6. Índices compuestos para consultas espaciales y de ocupación.
-- =============================================================================

SET search_path TO wms, public;

-- ─────────────────────────────────────────────────────────────────────────────
-- 1. EXTENDER wms.warehouse_sections CON METADATOS ESPACIALES Y DE ESTIBA
-- ─────────────────────────────────────────────────────────────────────────────

ALTER TABLE wms.warehouse_sections
    ADD COLUMN IF NOT EXISTS category VARCHAR(100) DEFAULT 'General',
    ADD COLUMN IF NOT EXISTS pos_fijas INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS capacidad_tarimas INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS factor_estiba VARCHAR(50) DEFAULT '22 tarimas/pos',
    ADD COLUMN IF NOT EXISTS notes TEXT,
    ADD COLUMN IF NOT EXISTS polygon_points TEXT,
    ADD COLUMN IF NOT EXISTS label_x NUMERIC(6,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS label_y NUMERIC(6,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS sublabel_x NUMERIC(6,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS sublabel_y NUMERIC(6,2) DEFAULT 0;

-- ─────────────────────────────────────────────────────────────────────────────
-- 2. REGISTRAR SECCIÓN K Y ACTUALIZAR COORDENADAS SVG EN SECCIONES EXISTENTES
-- ─────────────────────────────────────────────────────────────────────────────

-- 2.1 Asegurar existencia de SEC-ALM-K (Almacén K - Racks Libres & Anexo)
INSERT INTO wms.warehouse_sections (
    id, branch_id, code, name, category, pos_fijas, capacidad_tarimas, factor_estiba,
    notes, status, polygon_points, label_x, label_y, sublabel_x, sublabel_y,
    created_by, updated_by
) VALUES (
    'a83f0907-9fa5-4bdf-87db-2eb5e7683999',
    'b73f0907-9fa5-4bdf-87db-2eb5e7683936',
    'SEC-ALM-K',
    'Almacén K - Racks Libres & Anexo',
    'Almacén Anexo Exterior',
    181,
    3982,
    '22 tarimas/pos',
    'Racks libres para sobreflujo y consolidación de estiba pesada.',
    'ACTIVE',
    '600,166 734,130 734,369 600,369',
    667, 245, 667, 263,
    'SYSTEM', 'SYSTEM'
) ON CONFLICT (id) DO UPDATE SET
    code = EXCLUDED.code,
    name = EXCLUDED.name,
    category = EXCLUDED.category,
    pos_fijas = EXCLUDED.pos_fijas,
    capacidad_tarimas = EXCLUDED.capacidad_tarimas,
    factor_estiba = EXCLUDED.factor_estiba,
    polygon_points = EXCLUDED.polygon_points,
    label_x = EXCLUDED.label_x,
    label_y = EXCLUDED.label_y,
    sublabel_x = EXCLUDED.sublabel_x,
    sublabel_y = EXCLUDED.sublabel_y,
    updated_at = CURRENT_TIMESTAMP;

-- 2.2 Actualizar metadatos y polígonos de las naves activas existentes
UPDATE wms.warehouse_sections
SET category = 'Secos & Producto Terminado',
    pos_fijas = 170,
    capacidad_tarimas = 3740,
    factor_estiba = '22 tarimas/pos',
    notes = 'Área QUALAMEX en posiciones 149 a 170. Incluye Rampa 2.',
    polygon_points = '800,188 976,188 976,330 878,330 878,790 800,790',
    label_x = 865, label_y = 530, sublabel_x = 865, sublabel_y = 548,
    updated_at = CURRENT_TIMESTAMP
WHERE id = 'd7cceb3e-ad31-4ef4-bafa-f34b327ded9a'; -- SEC-ALM-A

UPDATE wms.warehouse_sections
SET category = 'Materia Prima & Insumos',
    pos_fijas = 38,
    capacidad_tarimas = 760,
    factor_estiba = '20 tarimas/pos',
    notes = 'Rampas 6, 7 y 8. Espacio para maniobra de montacargas.',
    polygon_points = '310,670 506,670 506,738 468,738 468,787 338,787 338,836 274,836 274,765 310,765',
    label_x = 412, label_y = 720, sublabel_x = 412, sublabel_y = 738,
    updated_at = CURRENT_TIMESTAMP
WHERE id = '28512354-5edf-4905-a042-61b0790c5277'; -- SEC-ALM-E

UPDATE wms.warehouse_sections
SET category = 'Empaque & Vidrio Industrial',
    pos_fijas = 120,
    capacidad_tarimas = 2640,
    factor_estiba = '22 tarimas/pos',
    notes = 'Incluye Oficinas de mantenimiento y almacén de cartón.',
    polygon_points = '600,369 722,369 722,760 635,760 635,824 597,824 597,710 600,710',
    label_x = 665, label_y = 585, sublabel_x = 665, sublabel_y = 603,
    updated_at = CURRENT_TIMESTAMP
WHERE id = '404a0fa1-fdea-4c30-932a-8e65ac6fb0a1'; -- SEC-ALM-D (F(D))

UPDATE wms.warehouse_sections
SET category = 'General Central & Palletizado',
    pos_fijas = 117,
    capacidad_tarimas = 2574,
    factor_estiba = '22 tarimas/pos',
    notes = 'Nave con columnas estructurales en cuadrícula.',
    polygon_points = '56,137 280,137 280,355 240,387 160,352 160,400 110,296 56,137',
    label_x = 195, label_y = 240, sublabel_x = 195, sublabel_y = 258,
    updated_at = CURRENT_TIMESTAMP
WHERE id = '67cb8d01-0a10-444b-b752-6ec7309b94e0'; -- SEC-ALM-G

UPDATE wms.warehouse_sections
SET category = 'Insumos Especiales',
    pos_fijas = 91,
    capacidad_tarimas = 2002,
    factor_estiba = '22 tarimas/pos',
    notes = 'Bahía longitudinal con pasillos de distribución central.',
    polygon_points = '412,298 506,252 506,670 412,670',
    label_x = 459, label_y = 470, sublabel_x = 459, sublabel_y = 488,
    updated_at = CURRENT_TIMESTAMP
WHERE id = 'd5703d81-cabd-4c62-8bbc-6ce96f7de184'; -- SEC-ALM-I

UPDATE wms.warehouse_sections
SET category = 'Granel & Tambores',
    pos_fijas = 56,
    capacidad_tarimas = 2240,
    factor_estiba = '40 tarimas/pos',
    notes = 'Alta capacidad de estiba por posición (40 tarimas/rack).',
    polygon_points = '280,323 310,323 312,298 412,298 412,670 310,670 310,642 280,642 280,494 298,494 298,355 280,355',
    label_x = 360, label_y = 470, sublabel_x = 360, sublabel_y = 488,
    updated_at = CURRENT_TIMESTAMP
WHERE id = 'f2cf444c-6b3a-4e08-bf40-f46d3b7597cf'; -- SEC-ALM-J (J(C))

UPDATE wms.warehouse_sections
SET category = 'Cuarentena & Retenidos',
    pos_fijas = 112,
    capacidad_tarimas = 2464,
    factor_estiba = '22 tarimas/pos',
    notes = '112 posiciones numeradas consecutivas 1 al 112.',
    polygon_points = '506,252 600,166 600,238 600,738 506,738',
    label_x = 553, label_y = 470, sublabel_x = 553, sublabel_y = 488,
    updated_at = CURRENT_TIMESTAMP
WHERE id = '7f028959-e3e1-4822-bf41-fe499e112c14'; -- SEC-ALM-L

UPDATE wms.warehouse_sections
SET category = 'Área Técnica',
    pos_fijas = 0,
    capacidad_tarimas = 0,
    factor_estiba = '--',
    notes = 'Pendiente de carga de archivo Excel de catálogo.',
    polygon_points = '228,17 298,17 298,87 268,91 268,135 228,135',
    label_x = 263, label_y = 60, sublabel_x = 263, sublabel_y = 75,
    updated_at = CURRENT_TIMESTAMP
WHERE id = 'ffd283a1-7568-45fb-9157-b3d96ecacdb4'; -- SEC-ALM-H

UPDATE wms.warehouse_sections
SET category = 'Área Futura',
    pos_fijas = 0,
    capacidad_tarimas = 0,
    factor_estiba = '--',
    notes = 'Pendiente de carga de archivo Excel de catálogo.',
    polygon_points = '734,130 853,52 853,188 734,188',
    label_x = 793, label_y = 125, sublabel_x = 793, sublabel_y = 140,
    updated_at = CURRENT_TIMESTAMP
WHERE id = '5e3d66ce-679b-40fd-a4c5-0a71731c1e56'; -- SEC-ALM-B

UPDATE wms.warehouse_sections
SET category = 'Área Futura',
    pos_fijas = 0,
    capacidad_tarimas = 0,
    factor_estiba = '--',
    notes = 'Pendiente de carga de archivo Excel de catálogo.',
    polygon_points = '722,369 800,369 800,760 722,760',
    label_x = 760, label_y = 585, sublabel_x = 760, sublabel_y = 600,
    updated_at = CURRENT_TIMESTAMP
WHERE id = 'b0d7b81d-2d2a-4f82-9f3e-9c90dcd7ca77'; -- SEC-ALM-C

-- ─────────────────────────────────────────────────────────────────────────────
-- 3. CATÁLOGO DE MOTIVOS DE BLOQUEO QM (wms.cat_block_reasons)
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS wms.cat_block_reasons (
    id UUID PRIMARY KEY DEFAULT wms.uuid_generate_v4(),
    code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(200) NOT NULL,
    category VARCHAR(50) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO wms.cat_block_reasons (code, description, category) VALUES
    ('QM_CONTAMINATION',   'Cuarentena QM — Sospecha de contaminación', 'QUALITY'),
    ('QM_INSPECTION',      'Cuarentena QM — Inspección de calidad en proceso', 'QUALITY'),
    ('QM_LAB_SAMPLE',      'Cuarentena QM — Muestra retenida para análisis de laboratorio', 'QUALITY'),
    ('MAINT_RACK_REPAIR',  'Mantenimiento — Reparación de rack o estructura', 'MAINTENANCE'),
    ('MAINT_SCHEDULED',    'Mantenimiento — Inspección técnica programada', 'MAINTENANCE'),
    ('CYCLE_COUNT',        'Inventario cíclico — Recuento en curso', 'INVENTORY'),
    ('PHYSICAL_DAMAGE',    'Daño físico — Producto con daño visible', 'SECURITY'),
    ('SPILL_HAZARD',       'Derrame o contaminación — Zona delimitada por seguridad', 'SECURITY'),
    ('ADMIN_REVIEW',       'Bloqueo administrativo — Pendiente de revisión por supervisor', 'ADMINISTRATIVE'),
    ('OVERWEIGHT_LIMIT',   'Exceso de peso — Sobrepasa límite de carga del rack', 'SECURITY')
ON CONFLICT (code) DO UPDATE SET
    description = EXCLUDED.description,
    category = EXCLUDED.category,
    updated_at = CURRENT_TIMESTAMP;

-- ─────────────────────────────────────────────────────────────────────────────
-- 4. TABLA DE ASOCIACIÓN SECCIÓN-SKUS PERMITIDOS (wms.warehouse_section_skus)
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS wms.warehouse_section_skus (
    id UUID PRIMARY KEY DEFAULT wms.uuid_generate_v4(),
    section_id UUID NOT NULL REFERENCES wms.warehouse_sections(id) ON DELETE CASCADE,
    sku_id UUID NOT NULL REFERENCES wms.products_sku(id) ON DELETE CASCADE,
    is_primary BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_section_sku UNIQUE (section_id, sku_id)
);

-- Vincular SKUs de Nestlé a las secciones según la pauta del demo
INSERT INTO wms.warehouse_section_skus (section_id, sku_id)
SELECT s.id, p.id
FROM wms.warehouse_sections s
CROSS JOIN wms.products_sku p
WHERE (s.code = 'SEC-ALM-A' AND p.code IN ('43211385', '43519988'))
   OR (s.code = 'SEC-ALM-E' AND p.code IN ('41165316', '44242440'))
   OR (s.code = 'SEC-ALM-D' AND p.code IN ('41165277', '44271527', '41165274'))
   OR (s.code = 'SEC-ALM-G' AND p.code IN ('44270022', '41165316', '44318043', '43140353'))
   OR (s.code = 'SEC-ALM-I' AND p.code IN ('43211385', '43759735', '44318043', '43510616'))
   OR (s.code = 'SEC-ALM-J' AND p.code IN ('41165272', '41165273', '41165275', '41165276', '43457162'))
   OR (s.code = 'SEC-ALM-L' AND p.code IN ('44271537', '43543406'))
   OR (s.code = 'SEC-ALM-K' AND p.code IN ('41165793', '43759734'))
ON CONFLICT (section_id, sku_id) DO NOTHING;

-- ─────────────────────────────────────────────────────────────────────────────
-- 5. SIEMBRA AUTOMATIZADA DE LAS 885 POSICIONES FÍSICAS EN wms.locations
-- ─────────────────────────────────────────────────────────────────────────────

CREATE OR REPLACE FUNCTION wms.fn_seed_warehouse_section_positions(
    p_branch_id UUID,
    p_section_id UUID,
    p_prefix VARCHAR,
    p_count INTEGER,
    p_capacity INTEGER
) RETURNS VOID AS $$
DECLARE
    i INTEGER;
    v_code VARCHAR(30);
    v_name VARCHAR(150);
    v_pos_str VARCHAR(10);
BEGIN
    FOR i IN 1..p_count LOOP
        v_pos_str := LPAD(i::TEXT, 3, '0');
        v_code    := 'POS-' || p_prefix || '-' || v_pos_str;
        v_name    := 'Posición ' || v_pos_str || ' — Nave ' || p_prefix;

        INSERT INTO wms.locations (
            id, branch_id, section_id, code, name, zone, aisle, rack, level, position,
            coord_x, coord_y, coord_z, type, status, capacity_units, current_occupancy,
            is_blocked, is_active, created_by, updated_by
        ) VALUES (
            wms.uuid_generate_v4(),
            p_branch_id,
            p_section_id,
            v_code,
            v_name,
            p_prefix,
            LPAD(((i - 1) / 20 + 1)::TEXT, 2, '0'), -- Pasillo simulado
            LPAD(((i - 1) % 10 + 1)::TEXT, 2, '0'), -- Rack simulado
            1,                                      -- Nivel 1 (piso/estiba)
            v_pos_str,
            ((i - 1) % 10) * 5,
            ((i - 1) / 10) * 5,
            1,
            'PALLET',
            'ACTIVE',
            p_capacity,
            0,
            FALSE,
            TRUE,
            'SYSTEM',
            'SYSTEM'
        ) ON CONFLICT (code) DO UPDATE SET
            section_id = EXCLUDED.section_id,
            capacity_units = EXCLUDED.capacity_units,
            status = 'ACTIVE',
            updated_at = CURRENT_TIMESTAMP;
    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- Ejecutar siembra para las 8 secciones activas (Total: 885 posiciones)
DO $$
DECLARE
    v_branch UUID := 'b73f0907-9fa5-4bdf-87db-2eb5e7683936';
BEGIN
    -- Nave A: 170 posiciones (Capacidad 22)
    PERFORM wms.fn_seed_warehouse_section_positions(v_branch, 'd7cceb3e-ad31-4ef4-bafa-f34b327ded9a', 'A', 170, 22);

    -- Nave E: 38 posiciones (Capacidad 20)
    PERFORM wms.fn_seed_warehouse_section_positions(v_branch, '28512354-5edf-4905-a042-61b0790c5277', 'E', 38, 20);

    -- Nave F(D): 120 posiciones (Capacidad 22)
    PERFORM wms.fn_seed_warehouse_section_positions(v_branch, '404a0fa1-fdea-4c30-932a-8e65ac6fb0a1', 'F-D', 120, 22);

    -- Nave G: 117 posiciones (Capacidad 22)
    PERFORM wms.fn_seed_warehouse_section_positions(v_branch, '67cb8d01-0a10-444b-b752-6ec7309b94e0', 'G', 117, 22);

    -- Nave I: 91 posiciones (Capacidad 22)
    PERFORM wms.fn_seed_warehouse_section_positions(v_branch, 'd5703d81-cabd-4c62-8bbc-6ce96f7de184', 'I', 91, 22);

    -- Nave J(C): 56 posiciones (Capacidad 40)
    PERFORM wms.fn_seed_warehouse_section_positions(v_branch, 'f2cf444c-6b3a-4e08-bf40-f46d3b7597cf', 'J-C', 56, 40);

    -- Nave L: 112 posiciones (Capacidad 22)
    PERFORM wms.fn_seed_warehouse_section_positions(v_branch, '7f028959-e3e1-4822-bf41-fe499e112c14', 'L', 112, 22);

    -- Nave K: 181 posiciones (Capacidad 22)
    PERFORM wms.fn_seed_warehouse_section_positions(v_branch, 'a83f0907-9fa5-4bdf-87db-2eb5e7683999', 'K', 181, 22);
END $$;

-- Eliminar función utilitaria de migración
DROP FUNCTION IF EXISTS wms.fn_seed_warehouse_section_positions(UUID, UUID, VARCHAR, INTEGER, INTEGER);

-- ─────────────────────────────────────────────────────────────────────────────
-- 6. ÍNDICES DE RENDIMIENTO PARA ACCESO ESPACIAL Y GESTIÓN EN TIEMPO REAL
-- ─────────────────────────────────────────────────────────────────────────────

CREATE INDEX IF NOT EXISTS idx_locations_section_status
    ON wms.locations (section_id, status)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_locations_code_search
    ON wms.locations (code varchar_pattern_ops);

CREATE INDEX IF NOT EXISTS idx_section_skus_section
    ON wms.warehouse_section_skus (section_id);
