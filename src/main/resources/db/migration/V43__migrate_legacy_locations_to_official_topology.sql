-- =============================================================================
-- V43__migrate_legacy_locations_to_official_topology.sql
-- Migración exhaustiva de ubicaciones heredadas (LOC-A-01-N1, LOC-ALM-B-01, etc.)
-- hacia las posiciones oficiales de los 12 almacenes (POS-A-001, POS-B-001, etc.)
-- =============================================================================

SET search_path TO wms, public;

DO $$
DECLARE
    v_branch_id UUID := 'b73f0907-9fa5-4bdf-87db-2eb5e7683936';
    v_pos_a1_id UUID;
    v_pos_a2_id UUID;
    v_pos_a3_id UUID;
    v_pos_a4_id UUID;
    v_pos_b1_id UUID;
    v_pos_b2_id UUID;
    v_pos_b3_id UUID;
    v_pos_b4_id UUID;
    v_pos_c1_id UUID;
    v_pos_d1_id UUID;
    v_pos_e1_id UUID;
    v_pos_g1_id UUID;
    v_pos_h1_id UUID;
    v_pos_i1_id UUID;
    v_pos_j1_id UUID;
    v_pos_l1_id UUID;
BEGIN
    -- 1. Obtener UUIDs de las posiciones oficiales destino
    SELECT id INTO v_pos_a1_id FROM wms.locations WHERE code = 'POS-A-001' AND branch_id = v_branch_id LIMIT 1;
    SELECT id INTO v_pos_a2_id FROM wms.locations WHERE code = 'POS-A-002' AND branch_id = v_branch_id LIMIT 1;
    SELECT id INTO v_pos_a3_id FROM wms.locations WHERE code = 'POS-A-003' AND branch_id = v_branch_id LIMIT 1;
    SELECT id INTO v_pos_a4_id FROM wms.locations WHERE code = 'POS-A-004' AND branch_id = v_branch_id LIMIT 1;

    SELECT id INTO v_pos_b1_id FROM wms.locations WHERE code = 'POS-B-001' AND branch_id = v_branch_id LIMIT 1;
    SELECT id INTO v_pos_b2_id FROM wms.locations WHERE code = 'POS-B-002' AND branch_id = v_branch_id LIMIT 1;
    SELECT id INTO v_pos_b3_id FROM wms.locations WHERE code = 'POS-B-003' AND branch_id = v_branch_id LIMIT 1;
    SELECT id INTO v_pos_b4_id FROM wms.locations WHERE code = 'POS-B-004' AND branch_id = v_branch_id LIMIT 1;

    SELECT id INTO v_pos_c1_id FROM wms.locations WHERE code = 'POS-C-001' AND branch_id = v_branch_id LIMIT 1;
    SELECT id INTO v_pos_d1_id FROM wms.locations WHERE code = 'POS-D-001' AND branch_id = v_branch_id LIMIT 1;
    SELECT id INTO v_pos_e1_id FROM wms.locations WHERE code = 'POS-E-001' AND branch_id = v_branch_id LIMIT 1;
    SELECT id INTO v_pos_g1_id FROM wms.locations WHERE code = 'POS-G-001' AND branch_id = v_branch_id LIMIT 1;
    SELECT id INTO v_pos_h1_id FROM wms.locations WHERE code = 'POS-H-001' AND branch_id = v_branch_id LIMIT 1;
    SELECT id INTO v_pos_i1_id FROM wms.locations WHERE code = 'POS-I-001' AND branch_id = v_branch_id LIMIT 1;
    SELECT id INTO v_pos_j1_id FROM wms.locations WHERE code = 'POS-J-001' AND branch_id = v_branch_id LIMIT 1;
    SELECT id INTO v_pos_l1_id FROM wms.locations WHERE code = 'POS-L-001' AND branch_id = v_branch_id LIMIT 1;

    IF v_pos_a1_id IS NULL THEN
        RAISE NOTICE 'No se encontró POS-A-001 en la sucursal %', v_branch_id;
        RETURN;
    END IF;

    -- 2. Función local de remapeo: actualizar inventory_items
    UPDATE wms.inventory_items i
    SET location_id = CASE
        WHEN l.code = 'LOC-A-01-N1' THEN v_pos_a1_id
        WHEN l.code = 'LOC-A-01-N2' THEN COALESCE(v_pos_a2_id, v_pos_a1_id)
        WHEN l.code = 'LOC-A-02-N1' THEN COALESCE(v_pos_a3_id, v_pos_a1_id)
        WHEN l.code = 'LOC-A-02-N2' THEN COALESCE(v_pos_a4_id, v_pos_a1_id)
        WHEN l.code = 'LOC-ALM-A-01' THEN v_pos_a1_id
        WHEN l.code = 'LOC-ALM-B-01' THEN COALESCE(v_pos_b1_id, v_pos_a1_id)
        WHEN l.code = 'LOC-B-01-N1' THEN COALESCE(v_pos_b1_id, v_pos_a1_id)
        WHEN l.code = 'LOC-B-01-N2' THEN COALESCE(v_pos_b2_id, v_pos_a1_id)
        WHEN l.code = 'LOC-B-02-N1' THEN COALESCE(v_pos_b3_id, v_pos_a1_id)
        WHEN l.code = 'LOC-B-02-N2' THEN COALESCE(v_pos_b4_id, v_pos_a1_id)
        WHEN l.code = 'LOC-ALM-C-01' THEN COALESCE(v_pos_c1_id, v_pos_a1_id)
        WHEN l.code = 'LOC-ALM-D-01' THEN COALESCE(v_pos_d1_id, v_pos_a1_id)
        WHEN l.code = 'LOC-ALM-E-01' THEN COALESCE(v_pos_e1_id, v_pos_a1_id)
        WHEN l.code = 'LOC-ALM-G-01' THEN COALESCE(v_pos_g1_id, v_pos_a1_id)
        WHEN l.code = 'LOC-ALM-H-01' THEN COALESCE(v_pos_h1_id, v_pos_a1_id)
        WHEN l.code = 'LOC-ALM-I-01' THEN COALESCE(v_pos_i1_id, v_pos_a1_id)
        WHEN l.code = 'LOC-ALM-J-01' THEN COALESCE(v_pos_j1_id, v_pos_a1_id)
        WHEN l.code = 'LOC-ALM-L-01' THEN COALESCE(v_pos_l1_id, v_pos_a1_id)
        ELSE v_pos_a1_id
    END
    FROM wms.locations l
    WHERE i.location_id = l.id
      AND (l.code LIKE 'LOC-%' OR l.code LIKE 'LOCALM-%');

    -- 3. Actualizar warehouse_receptions
    UPDATE wms.warehouse_receptions r
    SET storage_location_id = CASE
        WHEN l.code = 'LOC-A-01-N1' THEN v_pos_a1_id
        WHEN l.code = 'LOC-A-01-N2' THEN COALESCE(v_pos_a2_id, v_pos_a1_id)
        WHEN l.code = 'LOC-A-02-N1' THEN COALESCE(v_pos_a3_id, v_pos_a1_id)
        WHEN l.code = 'LOC-A-02-N2' THEN COALESCE(v_pos_a4_id, v_pos_a1_id)
        WHEN l.code = 'LOC-ALM-A-01' THEN v_pos_a1_id
        WHEN l.code = 'LOC-ALM-B-01' THEN COALESCE(v_pos_b1_id, v_pos_a1_id)
        WHEN l.code = 'LOC-ALM-C-01' THEN COALESCE(v_pos_c1_id, v_pos_a1_id)
        WHEN l.code = 'LOC-ALM-D-01' THEN COALESCE(v_pos_d1_id, v_pos_a1_id)
        WHEN l.code = 'LOC-ALM-E-01' THEN COALESCE(v_pos_e1_id, v_pos_a1_id)
        WHEN l.code = 'LOC-ALM-G-01' THEN COALESCE(v_pos_g1_id, v_pos_a1_id)
        WHEN l.code = 'LOC-ALM-H-01' THEN COALESCE(v_pos_h1_id, v_pos_a1_id)
        WHEN l.code = 'LOC-ALM-I-01' THEN COALESCE(v_pos_i1_id, v_pos_a1_id)
        WHEN l.code = 'LOC-ALM-J-01' THEN COALESCE(v_pos_j1_id, v_pos_a1_id)
        WHEN l.code = 'LOC-ALM-L-01' THEN COALESCE(v_pos_l1_id, v_pos_a1_id)
        ELSE v_pos_a1_id
    END
    FROM wms.locations l
    WHERE r.storage_location_id = l.id
      AND (l.code LIKE 'LOC-%' OR l.code LIKE 'LOCALM-%');

    -- 4. Actualizar inventory_movements (origen y destino)
    UPDATE wms.inventory_movements m
    SET from_location_id = CASE
        WHEN l.code = 'LOC-A-01-N1' THEN v_pos_a1_id
        WHEN l.code = 'LOC-A-01-N2' THEN COALESCE(v_pos_a2_id, v_pos_a1_id)
        WHEN l.code = 'LOC-A-02-N1' THEN COALESCE(v_pos_a3_id, v_pos_a1_id)
        WHEN l.code = 'LOC-A-02-N2' THEN COALESCE(v_pos_a4_id, v_pos_a1_id)
        WHEN l.code = 'LOC-ALM-B-01' THEN COALESCE(v_pos_b1_id, v_pos_a1_id)
        WHEN l.code = 'LOC-ALM-C-01' THEN COALESCE(v_pos_c1_id, v_pos_a1_id)
        ELSE v_pos_a1_id
    END
    FROM wms.locations l
    WHERE m.from_location_id = l.id
      AND (l.code LIKE 'LOC-%' OR l.code LIKE 'LOCALM-%');

    UPDATE wms.inventory_movements m
    SET to_location_id = CASE
        WHEN l.code = 'LOC-A-01-N1' THEN v_pos_a1_id
        WHEN l.code = 'LOC-A-01-N2' THEN COALESCE(v_pos_a2_id, v_pos_a1_id)
        WHEN l.code = 'LOC-A-02-N1' THEN COALESCE(v_pos_a3_id, v_pos_a1_id)
        WHEN l.code = 'LOC-A-02-N2' THEN COALESCE(v_pos_a4_id, v_pos_a1_id)
        WHEN l.code = 'LOC-ALM-B-01' THEN COALESCE(v_pos_b1_id, v_pos_a1_id)
        WHEN l.code = 'LOC-ALM-C-01' THEN COALESCE(v_pos_c1_id, v_pos_a1_id)
        ELSE v_pos_a1_id
    END
    FROM wms.locations l
    WHERE m.to_location_id = l.id
      AND (l.code LIKE 'LOC-%' OR l.code LIKE 'LOCALM-%');

    -- 5. Actualizar warehouse_transfers (origen y destino)
    UPDATE wms.warehouse_transfers t
    SET origin_location_id = CASE
        WHEN l.code = 'LOC-A-01-N1' THEN v_pos_a1_id
        WHEN l.code = 'LOC-A-01-N2' THEN COALESCE(v_pos_a2_id, v_pos_a1_id)
        WHEN l.code = 'LOC-A-02-N1' THEN COALESCE(v_pos_a3_id, v_pos_a1_id)
        WHEN l.code = 'LOC-A-02-N2' THEN COALESCE(v_pos_a4_id, v_pos_a1_id)
        WHEN l.code = 'LOC-ALM-B-01' THEN COALESCE(v_pos_b1_id, v_pos_a1_id)
        WHEN l.code = 'LOC-ALM-C-01' THEN COALESCE(v_pos_c1_id, v_pos_a1_id)
        ELSE v_pos_a1_id
    END
    FROM wms.locations l
    WHERE t.origin_location_id = l.id
      AND (l.code LIKE 'LOC-%' OR l.code LIKE 'LOCALM-%');

    UPDATE wms.warehouse_transfers t
    SET destination_location_id = CASE
        WHEN l.code = 'LOC-A-01-N1' THEN v_pos_a1_id
        WHEN l.code = 'LOC-A-01-N2' THEN COALESCE(v_pos_a2_id, v_pos_a1_id)
        WHEN l.code = 'LOC-A-02-N1' THEN COALESCE(v_pos_a3_id, v_pos_a1_id)
        WHEN l.code = 'LOC-A-02-N2' THEN COALESCE(v_pos_a4_id, v_pos_a1_id)
        WHEN l.code = 'LOC-ALM-B-01' THEN COALESCE(v_pos_b1_id, v_pos_a1_id)
        WHEN l.code = 'LOC-ALM-C-01' THEN COALESCE(v_pos_c1_id, v_pos_a1_id)
        ELSE v_pos_a1_id
    END
    FROM wms.locations l
    WHERE t.destination_location_id = l.id
      AND (l.code LIKE 'LOC-%' OR l.code LIKE 'LOCALM-%');

    -- 6. Actualizar warehouse_outbound_items
    UPDATE wms.warehouse_outbound_items
    SET location_code = 'POS-A-001'
    WHERE location_code LIKE 'LOC-%' OR location_code LIKE 'LOCALM-%';

    -- 7. Recalcular ocupación en wms.locations
    UPDATE wms.locations l
    SET current_occupancy = COALESCE((
        SELECT COUNT(*)
        FROM wms.inventory_items i
        WHERE i.location_id = l.id
          AND i.state NOT IN (50, 60, 70)
    ), 0)
    WHERE l.code LIKE 'POS-%';

    -- 8. Eliminar definitivamente las 18 ubicaciones obsoletas LOC-
    DELETE FROM wms.locations
    WHERE (code LIKE 'LOC-%' OR code LIKE 'LOCALM-%')
      AND type != 'RAMP';

    RAISE NOTICE 'Migración V43 completada con éxito: ubicaciones remapeadas a POS-* y limpiadas.';
END $$;
