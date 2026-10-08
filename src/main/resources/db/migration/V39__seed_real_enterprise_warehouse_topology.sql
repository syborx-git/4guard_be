-- =============================================================================
-- V39__seed_real_enterprise_warehouse_topology.sql
-- Topología Real de 12 Almacenes (Naves A, B, C, D, E, F, G, H, I, K, L, M)
-- 1,180 Posiciones Fijas (100% Base Nominal)
-- 474 Posiciones Temporales (Sobrecupo / Buffer)
-- 48 Posiciones de Precarga / Carga (4 por Almacén)
-- Total Físico: 1,702 Posiciones
-- =============================================================================

SET search_path TO wms, public;

-- 1. EXTENDER MODELO DE LOCATIONS CON CATEGORÍA DE POSICIÓN
ALTER TABLE wms.locations ADD COLUMN IF NOT EXISTS category VARCHAR(30) DEFAULT 'FIXED_STORAGE';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_locations_category'
    ) THEN
        ALTER TABLE wms.locations ADD CONSTRAINT chk_locations_category 
            CHECK (category IN ('FIXED_STORAGE', 'TEMPORARY_BUFFER', 'PRELOAD_STAGING'));
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_locations_category ON wms.locations(category);
CREATE INDEX IF NOT EXISTS idx_locations_zone_category ON wms.locations(zone, category);

-- 2. ASEGURAR SECCIONES / NAVES PARA LOS 12 ALMACENES
DO $$
DECLARE
    v_branch_id UUID := 'b73f0907-9fa5-4bdf-87db-2eb5e7683936';
    v_wh RECORD;
    v_sec_id UUID;
    v_wh_data CONSTANT JSONB := '[
        {"zone": "A", "name": "Almacén A", "fixed": 175, "temp": 50, "preload": 4},
        {"zone": "B", "name": "Almacén B", "fixed": 37,  "temp": 30, "preload": 4},
        {"zone": "C", "name": "Almacén C", "fixed": 72,  "temp": 50, "preload": 4},
        {"zone": "D", "name": "Almacén D", "fixed": 117, "temp": 50, "preload": 4},
        {"zone": "E", "name": "Almacén E", "fixed": 112, "temp": 30, "preload": 4},
        {"zone": "F", "name": "Almacén F", "fixed": 91,  "temp": 64, "preload": 4},
        {"zone": "G", "name": "Almacén G", "fixed": 38,  "temp": 50, "preload": 4},
        {"zone": "H", "name": "Almacén H", "fixed": 86,  "temp": 20, "preload": 4},
        {"zone": "I", "name": "Almacén I", "fixed": 117, "temp": 30, "preload": 4},
        {"zone": "K", "name": "Almacén K", "fixed": 22,  "temp": 30, "preload": 4},
        {"zone": "L", "name": "Almacén L", "fixed": 181, "temp": 30, "preload": 4},
        {"zone": "M", "name": "Almacén M", "fixed": 132, "temp": 40, "preload": 4}
    ]'::JSONB;
    
    i INT;
    v_pos_code VARCHAR(50);
    v_pos_name VARCHAR(150);
    v_aisle VARCHAR(10);
    v_rack VARCHAR(10);
    v_level INT;
    v_pos_num INT;
    v_fixed_count INT;
    v_temp_count INT;
    v_preload_count INT;
    v_zone_char VARCHAR(10);
    v_sec_name VARCHAR(150);
BEGIN
    -- Verificar si existe la sucursal de referencia o tomar la primera
    IF NOT EXISTS (SELECT 1 FROM wms.branches WHERE id = v_branch_id) THEN
        SELECT id INTO v_branch_id FROM wms.branches LIMIT 1;
    END IF;

    -- Iterar sobre los 12 almacenes (Upsert seguro sin romper FKs activas)
    FOR v_wh IN SELECT * FROM jsonb_to_recordset(v_wh_data) AS x(zone text, name text, fixed int, temp int, preload int)
    LOOP
        v_zone_char := v_wh.zone;
        v_sec_name := v_wh.name;
        v_fixed_count := v_wh.fixed;
        v_temp_count := v_wh.temp;
        v_preload_count := v_wh.preload;

        -- Crear o actualizar la sección del almacén
        SELECT id INTO v_sec_id FROM wms.warehouse_sections 
        WHERE branch_id = v_branch_id AND code = 'SEC-ALM-' || v_zone_char;

        IF v_sec_id IS NULL THEN
            v_sec_id := gen_random_uuid();
            INSERT INTO wms.warehouse_sections (id, branch_id, code, name, created_by, updated_by)
            VALUES (v_sec_id, v_branch_id, 'SEC-ALM-' || v_zone_char, v_sec_name, 'SYSTEM', 'SYSTEM');
        ELSE
            UPDATE wms.warehouse_sections 
            SET name = v_sec_name, updated_by = 'SYSTEM'
            WHERE id = v_sec_id;
        END IF;

        -- 1. Generar Posiciones Fijas (100% Base Nominal)
        FOR i IN 1..v_fixed_count LOOP
            v_pos_code := 'POS-' || v_zone_char || '-' || LPAD(i::text, 3, '0');
            v_aisle := LPAD(((i - 1) / 30 + 1)::text, 2, '0');
            v_rack := LPAD((((i - 1) % 30) / 3 + 1)::text, 2, '0');
            v_level := ((i - 1) % 3) + 1;
            v_pos_name := 'Almacén ' || v_zone_char || ' · Fija ' || LPAD(i::text, 3, '0');

            INSERT INTO wms.locations (
                id, branch_id, section_id, code, name, zone, aisle, rack, level, position,
                coord_x, coord_y, coord_z, type, capacity_units, current_occupancy,
                status, is_blocked, category, created_by, updated_by
            ) VALUES (
                gen_random_uuid(), v_branch_id, v_sec_id, v_pos_code, v_pos_name,
                v_zone_char, v_aisle, v_rack, v_level, LPAD(i::text, 3, '0'),
                ((i - 1) % 10) * 4, ((i - 1) / 10) * 3, v_level,
                'PALLET', 22, 0, 'ACTIVE', FALSE, 'FIXED_STORAGE', 'SYSTEM', 'SYSTEM'
            )
            ON CONFLICT (code) DO UPDATE SET
                section_id = EXCLUDED.section_id,
                name = EXCLUDED.name,
                zone = EXCLUDED.zone,
                category = 'FIXED_STORAGE',
                capacity_units = 22,
                status = 'ACTIVE',
                is_blocked = FALSE,
                updated_by = 'SYSTEM';
        END LOOP;

        -- 2. Generar Posiciones Temporales (Sobrecupo / Buffer)
        FOR i IN 1..v_temp_count LOOP
            v_pos_code := 'POS-' || v_zone_char || '-T' || LPAD(i::text, 2, '0');
            v_aisle := 'TMP';
            v_rack := LPAD(((i - 1) / 5 + 1)::text, 2, '0');
            v_level := 1;
            v_pos_name := 'Almacén ' || v_zone_char || ' · Temporal T' || LPAD(i::text, 2, '0');

            INSERT INTO wms.locations (
                id, branch_id, section_id, code, name, zone, aisle, rack, level, position,
                coord_x, coord_y, coord_z, type, capacity_units, current_occupancy,
                status, is_blocked, category, created_by, updated_by
            ) VALUES (
                gen_random_uuid(), v_branch_id, v_sec_id, v_pos_code, v_pos_name,
                v_zone_char, v_aisle, v_rack, v_level, 'T' || LPAD(i::text, 2, '0'),
                (i * 2), 99, 1,
                'PALLET', 22, 0, 'ACTIVE', FALSE, 'TEMPORARY_BUFFER', 'SYSTEM', 'SYSTEM'
            )
            ON CONFLICT (code) DO UPDATE SET
                section_id = EXCLUDED.section_id,
                name = EXCLUDED.name,
                zone = EXCLUDED.zone,
                category = 'TEMPORARY_BUFFER',
                capacity_units = 22,
                status = 'ACTIVE',
                is_blocked = FALSE,
                updated_by = 'SYSTEM';
        END LOOP;

        -- 3. Generar Posiciones de Precarga / Carga (4 por Almacén)
        FOR i IN 1..v_preload_count LOOP
            v_pos_code := 'POS-' || v_zone_char || '-PRE0' || i::text;
            v_aisle := 'PRE';
            v_rack := '01';
            v_level := 1;
            v_pos_name := 'Almacén ' || v_zone_char || ' · Precarga PRE-0' || i::text;

            INSERT INTO wms.locations (
                id, branch_id, section_id, code, name, zone, aisle, rack, level, position,
                coord_x, coord_y, coord_z, type, capacity_units, current_occupancy,
                status, is_blocked, category, created_by, updated_by
            ) VALUES (
                gen_random_uuid(), v_branch_id, v_sec_id, v_pos_code, v_pos_name,
                v_zone_char, v_aisle, v_rack, v_level, 'PRE0' || i::text,
                (i * 5), 0, 1,
                'PALLET', 22, 0, 'ACTIVE', FALSE, 'PRELOAD_STAGING', 'SYSTEM', 'SYSTEM'
            )
            ON CONFLICT (code) DO UPDATE SET
                section_id = EXCLUDED.section_id,
                name = EXCLUDED.name,
                zone = EXCLUDED.zone,
                category = 'PRELOAD_STAGING',
                capacity_units = 22,
                status = 'ACTIVE',
                is_blocked = FALSE,
                updated_by = 'SYSTEM';
        END LOOP;

    END LOOP;
END $$;
