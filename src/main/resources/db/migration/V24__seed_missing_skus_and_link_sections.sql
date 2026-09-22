-- ─────────────────────────────────────────────────────────────────────────────
-- V24__seed_missing_skus_and_link_sections.sql
-- Inserción de SKUs faltantes y asignación a secciones según catálogo de planta
-- ─────────────────────────────────────────────────────────────────────────────

-- 1. Insertar SKUs faltantes en wms.products_sku
INSERT INTO wms.products_sku (id, client_id, code, name, unit, category, description, created_at, updated_at, version)
SELECT 
    wms.uuid_generate_v4(),
    'c083251b-e210-494e-80f3-38814eaba992',
    '44242440',
    'ENVASE VIDRIO NESCAFE ICE 170G',
    'PZA',
    'FRASCO',
    'Envase Vidrio NESCAFE ICE 170g',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    1
WHERE NOT EXISTS (SELECT 1 FROM wms.products_sku WHERE code = '44242440');

INSERT INTO wms.products_sku (id, client_id, code, name, unit, category, description, created_at, updated_at, version)
SELECT 
    wms.uuid_generate_v4(),
    'c083251b-e210-494e-80f3-38814eaba992',
    '43140353',
    'ENVASE VIDRIO DAWN NESCAFE 300G MX',
    'PZA',
    'FRASCO',
    'Envase Vidrio Dawn NESCAFE 300g MX',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    1
WHERE NOT EXISTS (SELECT 1 FROM wms.products_sku WHERE code = '43140353');

INSERT INTO wms.products_sku (id, client_id, code, name, unit, category, description, created_at, updated_at, version)
SELECT 
    wms.uuid_generate_v4(),
    'c083251b-e210-494e-80f3-38814eaba992',
    '43759735',
    'ENVASE VIDRIO NESCAFE DOLCA 85G MX',
    'PZA',
    'FRASCO',
    'Envase Vidrio NESCAFE DOLCA 85g MX',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    1
WHERE NOT EXISTS (SELECT 1 FROM wms.products_sku WHERE code = '43759735');

-- 2. Vincular los SKUs a sus respectivas secciones
INSERT INTO wms.warehouse_section_skus (id, section_id, sku_id, is_primary, created_at)
SELECT wms.uuid_generate_v4(), s.id, p.id, true, CURRENT_TIMESTAMP
FROM wms.warehouse_sections s, wms.products_sku p
WHERE s.code = 'SEC-ALM-E' AND p.code = '44242440'
ON CONFLICT (section_id, sku_id) DO NOTHING;

INSERT INTO wms.warehouse_section_skus (id, section_id, sku_id, is_primary, created_at)
SELECT wms.uuid_generate_v4(), s.id, p.id, true, CURRENT_TIMESTAMP
FROM wms.warehouse_sections s, wms.products_sku p
WHERE s.code = 'SEC-ALM-G' AND p.code = '43140353'
ON CONFLICT (section_id, sku_id) DO NOTHING;

INSERT INTO wms.warehouse_section_skus (id, section_id, sku_id, is_primary, created_at)
SELECT wms.uuid_generate_v4(), s.id, p.id, true, CURRENT_TIMESTAMP
FROM wms.warehouse_sections s, wms.products_sku p
WHERE s.code = 'SEC-ALM-I' AND p.code = '43759735'
ON CONFLICT (section_id, sku_id) DO NOTHING;
