-- =============================================================================
-- V16: Seed Standard Storage Bays in wms.locations for Active Branch
-- Description: Registers physical storage rack bays (Pasillo A/B, Racks 01-04, Niveles 1-3)
--              under section 'SEC-ALM' (Almacenamiento General) for the 
--              active branch, providing real putaway capacity for automated slotting.
-- =============================================================================

SET search_path TO wms, public;

-- 1. Ensure SEC-ALM section exists
INSERT INTO wms.warehouse_sections (id, branch_id, code, name, status, created_by, updated_by)
VALUES (
    'd13f0907-9fa5-4bdf-87db-2eb5e7683912',
    'b73f0907-9fa5-4bdf-87db-2eb5e7683936',
    'SEC-ALM',
    'Almacenamiento General - Racks A-D',
    'ACTIVE',
    'SYSTEM',
    'SYSTEM'
)
ON CONFLICT (id) DO UPDATE SET 
    name = EXCLUDED.name,
    status = 'ACTIVE',
    updated_at = CURRENT_TIMESTAMP;

-- 2. Insert Standard Storage Locations (Bays)
INSERT INTO wms.locations (
    id, branch_id, section_id, code, name, zone, aisle, rack, level, position,
    coord_x, coord_y, coord_z, type, capacity_units, current_occupancy, is_blocked,
    status, created_by, updated_by
) VALUES
    ('e13f0907-9fa5-4bdf-87db-2eb5e7683901', 'b73f0907-9fa5-4bdf-87db-2eb5e7683936', 'd13f0907-9fa5-4bdf-87db-2eb5e7683912', 'LOC-A-01-N1', 'Pasillo A - Rack 01 - Nivel 1', 'ZA', '01', '01', 1, 'P01', 10, 10, 1, 'PALLET', 10, 0, FALSE, 'ACTIVE', 'SYSTEM', 'SYSTEM'),
    ('e13f0907-9fa5-4bdf-87db-2eb5e7683902', 'b73f0907-9fa5-4bdf-87db-2eb5e7683936', 'd13f0907-9fa5-4bdf-87db-2eb5e7683912', 'LOC-A-01-N2', 'Pasillo A - Rack 01 - Nivel 2', 'ZA', '01', '01', 2, 'P01', 10, 10, 2, 'PALLET', 10, 0, FALSE, 'ACTIVE', 'SYSTEM', 'SYSTEM'),
    ('e13f0907-9fa5-4bdf-87db-2eb5e7683903', 'b73f0907-9fa5-4bdf-87db-2eb5e7683936', 'd13f0907-9fa5-4bdf-87db-2eb5e7683912', 'LOC-A-02-N1', 'Pasillo A - Rack 02 - Nivel 1', 'ZA', '01', '02', 1, 'P01', 10, 20, 1, 'PALLET', 10, 0, FALSE, 'ACTIVE', 'SYSTEM', 'SYSTEM'),
    ('e13f0907-9fa5-4bdf-87db-2eb5e7683904', 'b73f0907-9fa5-4bdf-87db-2eb5e7683936', 'd13f0907-9fa5-4bdf-87db-2eb5e7683912', 'LOC-A-02-N2', 'Pasillo A - Rack 02 - Nivel 2', 'ZA', '01', '02', 2, 'P01', 10, 20, 2, 'PALLET', 10, 0, FALSE, 'ACTIVE', 'SYSTEM', 'SYSTEM'),
    ('e13f0907-9fa5-4bdf-87db-2eb5e7683905', 'b73f0907-9fa5-4bdf-87db-2eb5e7683936', 'd13f0907-9fa5-4bdf-87db-2eb5e7683912', 'LOC-B-01-N1', 'Pasillo B - Rack 01 - Nivel 1', 'ZB', '02', '01', 1, 'P01', 20, 10, 1, 'PALLET', 10, 0, FALSE, 'ACTIVE', 'SYSTEM', 'SYSTEM'),
    ('e13f0907-9fa5-4bdf-87db-2eb5e7683906', 'b73f0907-9fa5-4bdf-87db-2eb5e7683936', 'd13f0907-9fa5-4bdf-87db-2eb5e7683912', 'LOC-B-01-N2', 'Pasillo B - Rack 01 - Nivel 2', 'ZB', '02', '01', 2, 'P01', 20, 10, 2, 'PALLET', 10, 0, FALSE, 'ACTIVE', 'SYSTEM', 'SYSTEM'),
    ('e13f0907-9fa5-4bdf-87db-2eb5e7683907', 'b73f0907-9fa5-4bdf-87db-2eb5e7683936', 'd13f0907-9fa5-4bdf-87db-2eb5e7683912', 'LOC-B-02-N1', 'Pasillo B - Rack 02 - Nivel 1', 'ZB', '02', '02', 1, 'P01', 20, 20, 1, 'PALLET', 10, 0, FALSE, 'ACTIVE', 'SYSTEM', 'SYSTEM'),
    ('e13f0907-9fa5-4bdf-87db-2eb5e7683908', 'b73f0907-9fa5-4bdf-87db-2eb5e7683936', 'd13f0907-9fa5-4bdf-87db-2eb5e7683912', 'LOC-B-02-N2', 'Pasillo B - Rack 02 - Nivel 2', 'ZB', '02', '02', 2, 'P01', 20, 20, 2, 'PALLET', 10, 0, FALSE, 'ACTIVE', 'SYSTEM', 'SYSTEM')
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    code = EXCLUDED.code,
    type = 'PALLET',
    capacity_units = EXCLUDED.capacity_units,
    status = 'ACTIVE',
    updated_at = CURRENT_TIMESTAMP;
