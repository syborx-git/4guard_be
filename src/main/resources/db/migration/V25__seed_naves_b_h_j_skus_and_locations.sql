-- ─────────────────────────────────────────────────────────────────────────────
-- V25__seed_naves_b_h_j_skus_and_locations.sql
-- Actualización y homologación definitiva de Naves B, H, C y J según catálogos:
--   - Almacén B (SEC-ALM-B): 36 posiciones fijas (Capacidad 792 tarimas / Factor 22) + 5 SKUs
--   - Almacén H (SEC-ALM-H): 25 posiciones fijas (Capacidad 550 tarimas / Factor 22) + SKU 43940971
--   - Almacén C (SEC-ALM-C): Bahía vertical adyacente a nave oeste. 56 pos (Capacidad 2,240 / Factor 40) + Catálogo Culinarios
--   - Almacén J (SEC-ALM-J): Bahía este. 56 pos (Capacidad 1,680 / Factor 30) Granel & Tambores / Sobreflujo
-- ─────────────────────────────────────────────────────────────────────────────

-- 1. ACTUALIZAR METADATOS, CAPACIDADES Y POLÍGONOS EN wms.warehouse_sections

-- 1.1 Almacén B (SEC-ALM-B)
UPDATE wms.warehouse_sections
SET name = 'Almacén B (Cap. 792 Pallets / 36 Pos)',
    category = 'Materia Prima & Insumos',
    status = 'ACTIVE',
    pos_fijas = 36,
    capacidad_tarimas = 792,
    factor_estiba = '22 tarimas/pos',
    notes = 'Módulo superior derecho de diseño angular con Rampa 6. Almacenamiento de hojalata y vidrio.',
    polygon_points = '734,130 853,52 853,188 734,188',
    label_x = 793, label_y = 125, sublabel_x = 793, sublabel_y = 140,
    updated_at = CURRENT_TIMESTAMP
WHERE id = '5e3d66ce-679b-40fd-a4c5-0a71731c1e56';

-- 1.2 Almacén H (SEC-ALM-H)
UPDATE wms.warehouse_sections
SET name = 'Almacén H (Cap. 550 Pallets / 25 Pos)',
    category = 'Envase & Vidrio',
    status = 'ACTIVE',
    pos_fijas = 25,
    capacidad_tarimas = 550,
    factor_estiba = '22 tarimas/pos',
    notes = 'Bahía anexa vertical en el extremo superior izquierdo (Norte). Incluye área reservada para Varios Corrugados y Laminados.',
    polygon_points = '228,17 298,17 298,87 268,91 268,135 228,135',
    label_x = 263, label_y = 60, sublabel_x = 263, sublabel_y = 75,
    updated_at = CURRENT_TIMESTAMP
WHERE id = 'ffd283a1-7568-45fb-9157-b3d96ecacdb4';

-- 1.3 Almacén C (SEC-ALM-C) — Bahía vertical adyacente a la nave oeste (56 pos / 2,240 tarimas / Factor 40)
UPDATE wms.warehouse_sections
SET name = 'Almacén C (Cap. 2240 Pallets / 56 Pos)',
    category = 'Culinarios & Salsas',
    status = 'ACTIVE',
    pos_fijas = 56,
    capacidad_tarimas = 2240,
    factor_estiba = '40 tarimas/pos',
    notes = 'Bahía vertical adyacente a la nave oeste. Catálogo consolidado culinarios: Maggi, Crosse&Blackwell y Área de Cartón.',
    polygon_points = '280,323 310,323 312,298 412,298 412,670 310,670 310,642 280,642 280,494 298,494 298,355 280,355',
    label_x = 360, label_y = 470, sublabel_x = 360, sublabel_y = 488,
    updated_at = CURRENT_TIMESTAMP
WHERE id = 'b0d7b81d-2d2a-4f82-9f3e-9c90dcd7ca77';

-- 1.4 Almacén J (SEC-ALM-J) — Bahía este restaurada (56 pos / 1,680 tarimas / Factor 30)
UPDATE wms.warehouse_sections
SET name = 'Almacén J (Cap. 1680 Pallets / 56 Pos)',
    category = 'Granel & Tambores',
    status = 'ACTIVE',
    pos_fijas = 56,
    capacidad_tarimas = 1680,
    factor_estiba = '30 tarimas/pos',
    notes = 'Alta capacidad de estiba por posición (30 tarimas/rack). Racks libres para sobreflujo y consolidación de estiba pesada.',
    polygon_points = '722,369 800,369 800,760 722,760',
    label_x = 760, label_y = 585, sublabel_x = 760, sublabel_y = 600,
    updated_at = CURRENT_TIMESTAMP
WHERE id = 'f2cf444c-6b3a-4e08-bf40-f46d3b7597cf';


-- 2. GESTIÓN DE SKUs POR SECCIÓN (wms.warehouse_section_skus)

-- 2.1 Almacén B: Hojalata y Vidrio (5 SKUs)
INSERT INTO wms.warehouse_section_skus (id, section_id, sku_id, is_primary, created_at)
SELECT wms.uuid_generate_v4(), s.id, p.id, true, CURRENT_TIMESTAMP
FROM wms.warehouse_sections s
CROSS JOIN wms.products_sku p
WHERE s.code = 'SEC-ALM-B'
  AND p.code IN (
      '43622860', -- LATA HOJALATA CORTA PEELOFF D 153 MM
      '43624138', -- LATA HOJALATA PEEL OFF D153 MM
      '43800303', -- LATA HOJALATA DTR 126.7 MM
      '44512217', -- LATA HOJALATA LITHO DIA 126.7X149.9MM
      '43894859'  -- Envase de Vidrio Signature 250g Mx
  )
ON CONFLICT (section_id, sku_id) DO NOTHING;

-- 2.2 Almacén H: Vidrio Signature (1 SKU)
INSERT INTO wms.warehouse_section_skus (id, section_id, sku_id, is_primary, created_at)
SELECT wms.uuid_generate_v4(), s.id, p.id, true, CURRENT_TIMESTAMP
FROM wms.warehouse_sections s
CROSS JOIN wms.products_sku p
WHERE s.code = 'SEC-ALM-H'
  AND p.code = '43940971' -- Envase de Vidrio Signature 200g Mx
ON CONFLICT (section_id, sku_id) DO NOTHING;

-- 2.3 Desvincular SKUs Culinarios de Almacén J (SEC-ALM-J)
DELETE FROM wms.warehouse_section_skus
WHERE section_id = 'f2cf444c-6b3a-4e08-bf40-f46d3b7597cf';

-- 2.4 Asignar catálogo Culinarios completo a Almacén C (SEC-ALM-C)
INSERT INTO wms.warehouse_section_skus (id, section_id, sku_id, is_primary, created_at)
SELECT wms.uuid_generate_v4(), s.id, p.id, true, CURRENT_TIMESTAMP
FROM wms.warehouse_sections s
CROSS JOIN wms.products_sku p
WHERE s.code = 'SEC-ALM-C'
  AND p.code IN (
      '43799251', -- BOTELLA VIDRIO CROSSE&BLACKWELL 50ML N01
      '41165272', -- BOTELLA VIDRIO JUGOS Y SALSAS MAGGI 100ML
      '41165273', -- BOTELLA VIDRIO JUGOS Y SALSAS MAGGI 200ML
      '41165275', -- BOTELLA VIDRIO SALSA INGLESA C&B 160 G
      '41165276', -- BOTELLA VIDRIO SALSA INGLESA C&B 320 G
      '43457162'  -- BOTELLA VIDRIO MAGGI 50ML
  )
ON CONFLICT (section_id, sku_id) DO NOTHING;


-- 3. SIEMBRA Y REORGANIZACIÓN DE POSICIONES FÍSICAS (wms.locations)

-- 3.1 Limpiar ubicaciones híbridas anteriores POS-J-C-% si existen
DELETE FROM wms.locations WHERE code LIKE 'POS-J-C-%';

-- 3.2 Almacén B: 36 Posiciones consecutivas POS-B-001 a POS-B-036 (Capacidad unitaria: 22)
INSERT INTO wms.locations (
    id, branch_id, section_id, code, name, zone, aisle, rack, level, position,
    coord_x, coord_y, coord_z, type, status, capacity_units, current_occupancy,
    is_blocked, is_active, created_by, updated_by
)
SELECT
    wms.uuid_generate_v4(),
    'b73f0907-9fa5-4bdf-87db-2eb5e7683936'::uuid,
    '5e3d66ce-679b-40fd-a4c5-0a71731c1e56'::uuid,
    'POS-B-' || LPAD(i::text, 3, '0'),
    'Posición ' || LPAD(i::text, 3, '0') || ' — Nave B',
    'B',
    LPAD(((i - 1) / 20 + 1)::text, 2, '0'),
    LPAD(((i - 1) % 10 + 1)::text, 2, '0'),
    1,
    LPAD(i::text, 3, '0'),
    ((i - 1) % 10) * 5,
    ((i - 1) / 10) * 5,
    1,
    'PALLET',
    'ACTIVE',
    22,
    0,
    FALSE,
    TRUE,
    'SYSTEM',
    'SYSTEM'
FROM generate_series(1, 36) AS i
ON CONFLICT (code) DO UPDATE SET
    section_id = EXCLUDED.section_id,
    capacity_units = EXCLUDED.capacity_units,
    status = 'ACTIVE',
    updated_at = CURRENT_TIMESTAMP;

-- 3.3 Almacén H: 25 Posiciones consecutivas POS-H-001 a POS-H-025 (Capacidad unitaria: 22)
INSERT INTO wms.locations (
    id, branch_id, section_id, code, name, zone, aisle, rack, level, position,
    coord_x, coord_y, coord_z, type, status, capacity_units, current_occupancy,
    is_blocked, is_active, created_by, updated_by
)
SELECT
    wms.uuid_generate_v4(),
    'b73f0907-9fa5-4bdf-87db-2eb5e7683936'::uuid,
    'ffd283a1-7568-45fb-9157-b3d96ecacdb4'::uuid,
    'POS-H-' || LPAD(i::text, 3, '0'),
    'Posición ' || LPAD(i::text, 3, '0') || ' — Nave H',
    'H',
    LPAD(((i - 1) / 20 + 1)::text, 2, '0'),
    LPAD(((i - 1) % 10 + 1)::text, 2, '0'),
    1,
    LPAD(i::text, 3, '0'),
    ((i - 1) % 10) * 5,
    ((i - 1) / 10) * 5,
    1,
    'PALLET',
    'ACTIVE',
    22,
    0,
    FALSE,
    TRUE,
    'SYSTEM',
    'SYSTEM'
FROM generate_series(1, 25) AS i
ON CONFLICT (code) DO UPDATE SET
    section_id = EXCLUDED.section_id,
    capacity_units = EXCLUDED.capacity_units,
    status = 'ACTIVE',
    updated_at = CURRENT_TIMESTAMP;

-- 3.4 Almacén C: 56 Posiciones consecutivas POS-C-001 a POS-C-056 (Capacidad unitaria: 40)
INSERT INTO wms.locations (
    id, branch_id, section_id, code, name, zone, aisle, rack, level, position,
    coord_x, coord_y, coord_z, type, status, capacity_units, current_occupancy,
    is_blocked, is_active, created_by, updated_by
)
SELECT
    wms.uuid_generate_v4(),
    'b73f0907-9fa5-4bdf-87db-2eb5e7683936'::uuid,
    'b0d7b81d-2d2a-4f82-9f3e-9c90dcd7ca77'::uuid,
    'POS-C-' || LPAD(i::text, 3, '0'),
    'Posición ' || LPAD(i::text, 3, '0') || ' — Nave C',
    'C',
    LPAD(((i - 1) / 20 + 1)::text, 2, '0'),
    LPAD(((i - 1) % 10 + 1)::text, 2, '0'),
    1,
    LPAD(i::text, 3, '0'),
    ((i - 1) % 10) * 5,
    ((i - 1) / 10) * 5,
    1,
    'PALLET',
    'ACTIVE',
    40,
    0,
    FALSE,
    TRUE,
    'SYSTEM',
    'SYSTEM'
FROM generate_series(1, 56) AS i
ON CONFLICT (code) DO UPDATE SET
    section_id = EXCLUDED.section_id,
    capacity_units = EXCLUDED.capacity_units,
    status = 'ACTIVE',
    updated_at = CURRENT_TIMESTAMP;

-- 3.5 Almacén J: 56 Posiciones consecutivas POS-J-001 a POS-J-056 (Capacidad unitaria: 30)
INSERT INTO wms.locations (
    id, branch_id, section_id, code, name, zone, aisle, rack, level, position,
    coord_x, coord_y, coord_z, type, status, capacity_units, current_occupancy,
    is_blocked, is_active, created_by, updated_by
)
SELECT
    wms.uuid_generate_v4(),
    'b73f0907-9fa5-4bdf-87db-2eb5e7683936'::uuid,
    'f2cf444c-6b3a-4e08-bf40-f46d3b7597cf'::uuid,
    'POS-J-' || LPAD(i::text, 3, '0'),
    'Posición ' || LPAD(i::text, 3, '0') || ' — Nave J',
    'J',
    LPAD(((i - 1) / 20 + 1)::text, 2, '0'),
    LPAD(((i - 1) % 10 + 1)::text, 2, '0'),
    1,
    LPAD(i::text, 3, '0'),
    ((i - 1) % 10) * 5,
    ((i - 1) / 10) * 5,
    1,
    'PALLET',
    'ACTIVE',
    30,
    0,
    FALSE,
    TRUE,
    'SYSTEM',
    'SYSTEM'
FROM generate_series(1, 56) AS i
ON CONFLICT (code) DO UPDATE SET
    section_id = EXCLUDED.section_id,
    capacity_units = EXCLUDED.capacity_units,
    status = 'ACTIVE',
    updated_at = CURRENT_TIMESTAMP;
