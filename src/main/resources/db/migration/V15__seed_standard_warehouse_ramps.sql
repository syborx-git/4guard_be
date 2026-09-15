-- =============================================================================
-- V15: Seed 12 Real Operational Warehouse Ramps in wms.locations
-- Description: Registers the 12 physical warehouse ramps (Rampa 01 to Rampa 12)
--              under section 'SEC-RAMP' (Andenes de Carga y Descarga) for the 
--              active branch, providing full DB persistence for Inbound/Outbound.
-- =============================================================================

SET search_path TO wms, public;

-- 1. Ensure SEC-RAMP section exists
INSERT INTO wms.warehouse_sections (id, branch_id, code, name, status, created_by, updated_by)
VALUES (
    'd13f0907-9fa5-4bdf-87db-2eb5e7683913',
    'b73f0907-9fa5-4bdf-87db-2eb5e7683936',
    'SEC-RAMP',
    'Andenes de Carga y Descarga',
    'ACTIVE',
    'SYSTEM',
    'SYSTEM'
)
ON CONFLICT (id) DO UPDATE SET 
    name = EXCLUDED.name,
    status = 'ACTIVE',
    updated_at = CURRENT_TIMESTAMP;

-- 2. Insert the 12 Standard Warehouse Ramps
INSERT INTO wms.locations (
    id, branch_id, section_id, code, name, zone, aisle, rack, level, position,
    coord_x, coord_y, coord_z, type, capacity_units, current_occupancy, is_blocked,
    status, created_by, updated_by
) VALUES
    ('e13f0907-9fa5-4bdf-87db-2eb5e7683925', 'b73f0907-9fa5-4bdf-87db-2eb5e7683936', 'd13f0907-9fa5-4bdf-87db-2eb5e7683913', 'LOC-RAMP-01', 'Rampa 01', 'ZC', '00', '00', 0, 'R01', 1, 1, 0, 'RAMP', 1, 0, FALSE, 'AVAILABLE', 'SYSTEM', 'SYSTEM'),
    (wms.fn_deterministic_uuid_v4('LOC-RAMP-02'), 'b73f0907-9fa5-4bdf-87db-2eb5e7683936', 'd13f0907-9fa5-4bdf-87db-2eb5e7683913', 'LOC-RAMP-02', 'Rampa 02', 'ZC', '00', '00', 0, 'R02', 1, 2, 0, 'RAMP', 1, 0, FALSE, 'AVAILABLE', 'SYSTEM', 'SYSTEM'),
    (wms.fn_deterministic_uuid_v4('LOC-RAMP-03'), 'b73f0907-9fa5-4bdf-87db-2eb5e7683936', 'd13f0907-9fa5-4bdf-87db-2eb5e7683913', 'LOC-RAMP-03', 'Rampa 03', 'ZC', '00', '00', 0, 'R03', 1, 3, 0, 'RAMP', 1, 0, FALSE, 'AVAILABLE', 'SYSTEM', 'SYSTEM'),
    (wms.fn_deterministic_uuid_v4('LOC-RAMP-04'), 'b73f0907-9fa5-4bdf-87db-2eb5e7683936', 'd13f0907-9fa5-4bdf-87db-2eb5e7683913', 'LOC-RAMP-04', 'Rampa 04', 'ZC', '00', '00', 0, 'R04', 1, 4, 0, 'RAMP', 1, 0, FALSE, 'AVAILABLE', 'SYSTEM', 'SYSTEM'),
    (wms.fn_deterministic_uuid_v4('LOC-RAMP-05'), 'b73f0907-9fa5-4bdf-87db-2eb5e7683936', 'd13f0907-9fa5-4bdf-87db-2eb5e7683913', 'LOC-RAMP-05', 'Rampa 05', 'ZC', '00', '00', 0, 'R05', 1, 5, 0, 'RAMP', 1, 0, FALSE, 'AVAILABLE', 'SYSTEM', 'SYSTEM'),
    (wms.fn_deterministic_uuid_v4('LOC-RAMP-06'), 'b73f0907-9fa5-4bdf-87db-2eb5e7683936', 'd13f0907-9fa5-4bdf-87db-2eb5e7683913', 'LOC-RAMP-06', 'Rampa 06', 'ZC', '00', '00', 0, 'R06', 1, 6, 0, 'RAMP', 1, 0, FALSE, 'AVAILABLE', 'SYSTEM', 'SYSTEM'),
    (wms.fn_deterministic_uuid_v4('LOC-RAMP-07'), 'b73f0907-9fa5-4bdf-87db-2eb5e7683936', 'd13f0907-9fa5-4bdf-87db-2eb5e7683913', 'LOC-RAMP-07', 'Rampa 07', 'ZC', '00', '00', 0, 'R07', 1, 7, 0, 'RAMP', 1, 0, FALSE, 'AVAILABLE', 'SYSTEM', 'SYSTEM'),
    (wms.fn_deterministic_uuid_v4('LOC-RAMP-08'), 'b73f0907-9fa5-4bdf-87db-2eb5e7683936', 'd13f0907-9fa5-4bdf-87db-2eb5e7683913', 'LOC-RAMP-08', 'Rampa 08', 'ZC', '00', '00', 0, 'R08', 1, 8, 0, 'RAMP', 1, 0, FALSE, 'AVAILABLE', 'SYSTEM', 'SYSTEM'),
    (wms.fn_deterministic_uuid_v4('LOC-RAMP-09'), 'b73f0907-9fa5-4bdf-87db-2eb5e7683936', 'd13f0907-9fa5-4bdf-87db-2eb5e7683913', 'LOC-RAMP-09', 'Rampa 09', 'ZC', '00', '00', 0, 'R09', 1, 9, 0, 'RAMP', 1, 0, FALSE, 'AVAILABLE', 'SYSTEM', 'SYSTEM'),
    (wms.fn_deterministic_uuid_v4('LOC-RAMP-10'), 'b73f0907-9fa5-4bdf-87db-2eb5e7683936', 'd13f0907-9fa5-4bdf-87db-2eb5e7683913', 'LOC-RAMP-10', 'Rampa 10', 'ZC', '00', '00', 0, 'R10', 1, 10, 0, 'RAMP', 1, 0, FALSE, 'AVAILABLE', 'SYSTEM', 'SYSTEM'),
    (wms.fn_deterministic_uuid_v4('LOC-RAMP-11'), 'b73f0907-9fa5-4bdf-87db-2eb5e7683936', 'd13f0907-9fa5-4bdf-87db-2eb5e7683913', 'LOC-RAMP-11', 'Rampa 11', 'ZC', '00', '00', 0, 'R11', 1, 11, 0, 'RAMP', 1, 0, FALSE, 'AVAILABLE', 'SYSTEM', 'SYSTEM'),
    (wms.fn_deterministic_uuid_v4('LOC-RAMP-12'), 'b73f0907-9fa5-4bdf-87db-2eb5e7683936', 'd13f0907-9fa5-4bdf-87db-2eb5e7683913', 'LOC-RAMP-12', 'Rampa 12', 'ZC', '00', '00', 0, 'R12', 1, 12, 0, 'RAMP', 1, 0, FALSE, 'AVAILABLE', 'SYSTEM', 'SYSTEM')
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    code = EXCLUDED.code,
    type = 'RAMP',
    updated_at = CURRENT_TIMESTAMP;
