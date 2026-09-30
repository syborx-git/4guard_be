-- =============================================================================
-- 4GUARD WMS — Flyway Migration V27
-- Archivo: V27__ensure_warehouse_sections_topology_columns.sql
-- Módulo: Asegurar columnas de topología 2D en wms.warehouse_sections (HU-048 / HU-127)
-- =============================================================================

SET search_path TO wms, public;

-- 1. Asegurar columnas en wms.warehouse_sections
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

-- 2. Asegurar existencia de SEC-ALM-K
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

-- 3. Actualizar metadatos y polígonos de las naves activas existentes
UPDATE wms.warehouse_sections
SET category = 'Secos & Producto Terminado',
    pos_fijas = 170,
    capacidad_tarimas = 3740,
    factor_estiba = '22 tarimas/pos',
    notes = 'Área QUALAMEX en posiciones 149 a 170. Incluye Rampa 2.',
    polygon_points = '800,188 976,188 976,330 878,330 878,790 800,790',
    label_x = 865, label_y = 530, sublabel_x = 865, sublabel_y = 548,
    updated_at = CURRENT_TIMESTAMP
WHERE id = 'd7cceb3e-ad31-4ef4-bafa-f34b327ded9a';

UPDATE wms.warehouse_sections
SET category = 'Materia Prima & Insumos',
    pos_fijas = 38,
    capacidad_tarimas = 760,
    factor_estiba = '20 tarimas/pos',
    notes = 'Rampas 6, 7 y 8. Espacio para maniobra de montacargas.',
    polygon_points = '310,670 506,670 506,738 468,738 468,787 338,787 338,836 274,836 274,765 310,765',
    label_x = 412, label_y = 720, sublabel_x = 412, sublabel_y = 738,
    updated_at = CURRENT_TIMESTAMP
WHERE id = '28512354-5edf-4905-a042-61b0790c5277';

UPDATE wms.warehouse_sections
SET category = 'Empaque & Vidrio Industrial',
    pos_fijas = 120,
    capacidad_tarimas = 2640,
    factor_estiba = '22 tarimas/pos',
    notes = 'Incluye Oficinas de mantenimiento y almacén de cartón.',
    polygon_points = '600,369 722,369 722,760 635,760 635,824 597,824 597,710 600,710',
    label_x = 665, label_y = 585, sublabel_x = 665, sublabel_y = 603,
    updated_at = CURRENT_TIMESTAMP
WHERE id = '404a0fa1-fdea-4c30-932a-8e65ac6fb0a1';

UPDATE wms.warehouse_sections
SET category = 'General Central & Palletizado',
    pos_fijas = 117,
    capacidad_tarimas = 2574,
    factor_estiba = '22 tarimas/pos',
    notes = 'Nave con columnas estructurales en cuadrícula.',
    polygon_points = '56,137 280,137 280,355 240,387 160,352 160,400 110,296 56,137',
    label_x = 195, label_y = 240, sublabel_x = 195, sublabel_y = 258,
    updated_at = CURRENT_TIMESTAMP
WHERE id = '67cb8d01-0a10-444b-b752-6ec7309b94e0';

UPDATE wms.warehouse_sections
SET category = 'Insumos Especiales',
    pos_fijas = 91,
    capacidad_tarimas = 2002,
    factor_estiba = '22 tarimas/pos',
    notes = 'Bahía longitudinal con pasillos de distribución central.',
    polygon_points = '412,298 506,252 506,670 412,670',
    label_x = 459, label_y = 470, sublabel_x = 459, sublabel_y = 488,
    updated_at = CURRENT_TIMESTAMP
WHERE id = 'd5703d81-cabd-4c62-8bbc-6ce96f7de184';

UPDATE wms.warehouse_sections
SET category = 'Granel & Tambores',
    pos_fijas = 56,
    capacidad_tarimas = 2240,
    factor_estiba = '40 tarimas/pos',
    notes = 'Alta capacidad de estiba por posición (40 tarimas/rack).',
    polygon_points = '280,323 310,323 312,298 412,298 412,670 310,670 310,642 280,642 280,494 298,494 298,355 280,355',
    label_x = 360, label_y = 470, sublabel_x = 360, sublabel_y = 488,
    updated_at = CURRENT_TIMESTAMP
WHERE id = 'f2cf444c-6b3a-4e08-bf40-f46d3b7597cf';

UPDATE wms.warehouse_sections
SET category = 'Cuarentena & Retenidos',
    pos_fijas = 112,
    capacidad_tarimas = 2464,
    factor_estiba = '22 tarimas/pos',
    notes = '112 posiciones numeradas consecutivas 1 al 112.',
    polygon_points = '506,252 600,166 600,238 600,738 506,738',
    label_x = 553, label_y = 470, sublabel_x = 553, sublabel_y = 488,
    updated_at = CURRENT_TIMESTAMP
WHERE id = '7f028959-e3e1-4822-bf41-fe499e112c14';
