-- =============================================================================
-- V42__align_warehouse_sections_and_positions_official_topology.sql
-- Alineación oficial de Topología de 12 Almacenes (A, B, C, D, E, F, G, H, I, J, K, L)
-- según matriz validada:
-- A: 175 Fijas, 50 Temp, 4 Carga (Total 229)
-- B:  37 Fijas, 30 Temp, 4 Carga (Total  71)
-- C:  72 Fijas, 50 Temp, 4 Carga (Total 126)
-- D: 117 Fijas, 50 Temp, 4 Carga (Total 171)
-- E: 112 Fijas, 30 Temp, 4 Carga (Total 146)
-- F:  91 Fijas, 64 Temp, 4 Carga (Total 159)
-- G:  38 Fijas, 50 Temp, 4 Carga (Total  92)
-- H:  86 Fijas, 20 Temp, 4 Carga (Total 110)
-- I: 117 Fijas, 30 Temp, 4 Carga (Total 151)
-- J:  22 Fijas, 30 Temp, 4 Carga (Total  56)
-- K: 181 Fijas, 30 Temp, 4 Carga (Total 215)
-- L: 132 Fijas, 40 Temp, 4 Carga (Total 176)
-- Total Nominal Fijas: 1,180 | Temporales: 474 | Carga: 48 | Total General: 1,702
-- =============================================================================

SET search_path TO wms, public;

DO $$
DECLARE
    v_branch_id UUID := 'b73f0907-9fa5-4bdf-87db-2eb5e7683936';
    v_wh RECORD;
    v_sec_id UUID;
    v_wh_data CONSTANT JSONB := '[
        {"zone": "A", "name": "Almacén A", "fixed": 175, "temp": 50, "preload": 4, "poly": "265,125 588,125 588,365 265,365", "lx": 426, "ly": 235, "slx": 426, "sly": 260},
        {"zone": "B", "name": "Almacén B", "fixed": 37,  "temp": 30, "preload": 4, "poly": "265,670 420,670 420,840 265,840", "lx": 342, "ly": 745, "slx": 342, "sly": 765},
        {"zone": "C", "name": "Almacén C", "fixed": 72,  "temp": 50, "preload": 4, "poly": "430,670 588,670 588,840 430,840", "lx": 509, "ly": 745, "slx": 509, "sly": 765},
        {"zone": "D", "name": "Almacén D", "fixed": 117, "temp": 50, "preload": 4, "poly": "602,125 783,125 783,245 602,245", "lx": 692, "ly": 180, "slx": 692, "sly": 200},
        {"zone": "E", "name": "Almacén E", "fixed": 112, "temp": 30, "preload": 4, "poly": "602,255 783,255 783,365 602,365", "lx": 692, "ly": 305, "slx": 692, "sly": 325},
        {"zone": "F", "name": "Almacén F", "fixed": 91,  "temp": 64, "preload": 4, "poly": "797,125 890,125 890,365 797,365", "lx": 843, "ly": 235, "slx": 843, "sly": 260},
        {"zone": "G", "name": "Almacén G", "fixed": 38,  "temp": 50, "preload": 4, "poly": "897,125 973,125 973,365 897,365", "lx": 935, "ly": 235, "slx": 935, "sly": 260},
        {"zone": "H", "name": "Almacén H", "fixed": 86,  "temp": 20, "preload": 4, "poly": "797,670 973,670 973,840 797,840", "lx": 885, "ly": 745, "slx": 885, "sly": 765},
        {"zone": "I", "name": "Almacén I", "fixed": 117, "temp": 30, "preload": 4, "poly": "265,375 588,375 588,660 265,660", "lx": 426, "ly": 505, "slx": 426, "sly": 530},
        {"zone": "J", "name": "Almacén J", "fixed": 22,  "temp": 30, "preload": 4, "poly": "897,375 973,375 973,660 897,660", "lx": 935, "ly": 505, "slx": 935, "sly": 530},
        {"zone": "K", "name": "Almacén K", "fixed": 181, "temp": 30, "preload": 4, "poly": "797,375 890,375 890,660 797,660", "lx": 843, "ly": 505, "slx": 843, "sly": 530},
        {"zone": "L", "name": "Almacén L", "fixed": 132, "temp": 40, "preload": 4, "poly": "602,375 783,375 783,660 602,660", "lx": 692, "ly": 505, "slx": 692, "sly": 530}
    ]'::JSONB;

    i INT;
    v_pos_code VARCHAR(50);
    v_pos_name VARCHAR(150);
    v_aisle VARCHAR(10);
    v_rack VARCHAR(10);
    v_level INT;
    v_fixed_count INT;
    v_temp_count INT;
    v_preload_count INT;
    v_zone_char VARCHAR(10);
    v_sec_name VARCHAR(150);
BEGIN
    IF NOT EXISTS (SELECT 1 FROM wms.branches WHERE id = v_branch_id) THEN
        SELECT id INTO v_branch_id FROM wms.branches LIMIT 1;
    END IF;

    -- 1. Desactivar y limpiar sección M si existiera (reemplazada formalmente por L según matriz)
    UPDATE wms.warehouse_sections 
    SET status = 'INACTIVE', pos_fijas = 0, capacidad_tarimas = 0, updated_by = 'SYSTEM'
    WHERE code = 'SEC-ALM-M';

    DELETE FROM wms.locations 
    WHERE zone = 'M' AND (current_occupancy IS NULL OR current_occupancy = 0);

    -- 2. Limpieza de posiciones sobrantes fuera del cupo oficial en G, J y L
    DELETE FROM wms.locations 
    WHERE zone = 'G' AND code ~ '^POS-G-[0-9]+$' 
      AND (substring(code from '^POS-G-([0-9]+)$')::int > 38)
      AND (current_occupancy IS NULL OR current_occupancy = 0);

    DELETE FROM wms.locations 
    WHERE zone = 'J' AND code ~ '^POS-J-[0-9]+$' 
      AND (substring(code from '^POS-J-([0-9]+)$')::int > 22)
      AND (current_occupancy IS NULL OR current_occupancy = 0);

    DELETE FROM wms.locations 
    WHERE zone = 'L' AND code ~ '^POS-L-[0-9]+$' 
      AND (substring(code from '^POS-L-([0-9]+)$')::int > 132)
      AND (current_occupancy IS NULL OR current_occupancy = 0);

    DELETE FROM wms.locations 
    WHERE code LIKE 'POS-F-D%' AND (current_occupancy IS NULL OR current_occupancy = 0);

    -- 3. Upsert de las 12 secciones oficiales y generación exacta de sus cuadrículas
    FOR v_wh IN SELECT * FROM jsonb_to_recordset(v_wh_data) AS x(
        zone text, name text, fixed int, temp int, preload int, poly text, lx numeric, ly numeric, slx numeric, sly numeric
    )
    LOOP
        v_zone_char := v_wh.zone;
        v_sec_name := v_wh.name;
        v_fixed_count := v_wh.fixed;
        v_temp_count := v_wh.temp;
        v_preload_count := v_wh.preload;

        SELECT id INTO v_sec_id FROM wms.warehouse_sections 
        WHERE branch_id = v_branch_id AND code = 'SEC-ALM-' || v_zone_char;

        IF v_sec_id IS NULL THEN
            v_sec_id := gen_random_uuid();
            INSERT INTO wms.warehouse_sections (
                id, branch_id, code, name, pos_fijas, capacidad_tarimas, factor_estiba,
                notes, status, polygon_points, label_x, label_y, sublabel_x, sublabel_y,
                created_by, updated_by
            ) VALUES (
                v_sec_id, v_branch_id, 'SEC-ALM-' || v_zone_char, v_sec_name,
                v_fixed_count, v_fixed_count * 22, '22 tarimas/pos',
                v_fixed_count || ' Fijas · ' || v_temp_count || ' Temporales · ' || v_preload_count || ' Precarga',
                'ACTIVE', v_wh.poly, v_wh.lx, v_wh.ly, v_wh.slx, v_wh.sly,
                'SYSTEM', 'SYSTEM'
            );
        ELSE
            UPDATE wms.warehouse_sections 
            SET name = v_sec_name,
                pos_fijas = v_fixed_count,
                capacidad_tarimas = v_fixed_count * 22,
                factor_estiba = '22 tarimas/pos',
                notes = v_fixed_count || ' Fijas · ' || v_temp_count || ' Temporales · ' || v_preload_count || ' Precarga',
                status = 'ACTIVE',
                polygon_points = v_wh.poly,
                label_x = v_wh.lx,
                label_y = v_wh.ly,
                sublabel_x = v_wh.slx,
                sublabel_y = v_wh.sly,
                updated_by = 'SYSTEM'
            WHERE id = v_sec_id;
        END IF;

        -- 3.1 Generar Fijas
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

        -- 3.2 Generar Temporales
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

        -- 3.3 Generar Carga / Precarga
        FOR i IN 1..v_preload_count LOOP
            v_pos_code := 'POS-' || v_zone_char || '-PRE0' || i::text;
            v_aisle := 'PRE';
            v_rack := '01';
            v_level := 1;
            v_pos_name := 'Almacén ' || v_zone_char || ' · Precarga PRE0' || i::text;

            INSERT INTO wms.locations (
                id, branch_id, section_id, code, name, zone, aisle, rack, level, position,
                coord_x, coord_y, coord_z, type, capacity_units, current_occupancy,
                status, is_blocked, category, created_by, updated_by
            ) VALUES (
                gen_random_uuid(), v_branch_id, v_sec_id, v_pos_code, v_pos_name,
                v_zone_char, v_aisle, v_rack, v_level, 'PRE0' || i::text,
                (i * 5), 105, 1,
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
