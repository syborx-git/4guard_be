-- =============================================================================
-- V17: Add Shifts and Schedules Management Schema (HU-140)
-- Author: 4GUARD Engineering Team
-- Description: Creates wms_shifts, shift_operating_days, user_shifts tables,
--              registers RBAC permissions, and seeds 5 test dummy shifts
--              linked to existing branch CDMX-01 and users.
-- =============================================================================

SET search_path TO wms, public;

-- 1. Main Shifts Table
CREATE TABLE wms.wms_shifts (
    id                   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code                 VARCHAR(30)  NOT NULL,
    name                 VARCHAR(100) NOT NULL,
    description          TEXT,
    start_time           TIME         NOT NULL,
    end_time             TIME         NOT NULL,
    rest_break_minutes   INT          NOT NULL DEFAULT 0,
    tolerance_minutes    INT          NOT NULL DEFAULT 0,
    is_overnight         BOOLEAN      NOT NULL DEFAULT FALSE,
    net_duration_minutes INT          GENERATED ALWAYS AS (
        CASE WHEN end_time < start_time
             THEN (EXTRACT(EPOCH FROM (end_time + INTERVAL '24 hours' - start_time)) / 60)::INT - rest_break_minutes
             ELSE (EXTRACT(EPOCH FROM (end_time - start_time)) / 60)::INT - rest_break_minutes
        END
    ) STORED,
    status               VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    scope_type           VARCHAR(20)  NOT NULL DEFAULT 'BRANCH',
    branch_id            UUID         REFERENCES wms.branches(id) ON DELETE SET NULL,
    warehouse_section_id UUID         REFERENCES wms.warehouse_sections(id) ON DELETE SET NULL,
    is_deleted           BOOLEAN      NOT NULL DEFAULT FALSE,
    version              BIGINT       DEFAULT 1,
    created_at           TIMESTAMPTZ  DEFAULT NOW(),
    updated_at           TIMESTAMPTZ  DEFAULT NOW(),
    created_by           VARCHAR(36)  NOT NULL DEFAULT 'SYSTEM',
    updated_by           VARCHAR(36),
    CONSTRAINT uk_wms_shifts_code_branch UNIQUE (code, branch_id)
);

CREATE INDEX idx_wms_shifts_branch ON wms.wms_shifts(branch_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_wms_shifts_status ON wms.wms_shifts(status) WHERE is_deleted = FALSE;
CREATE INDEX idx_wms_shifts_scope  ON wms.wms_shifts(scope_type, branch_id) WHERE is_deleted = FALSE;

-- 2. Shift Operating Days (1:N)
CREATE TABLE wms.shift_operating_days (
    shift_id    UUID        NOT NULL REFERENCES wms.wms_shifts(id) ON DELETE CASCADE,
    day_of_week VARCHAR(15) NOT NULL,
    PRIMARY KEY (shift_id, day_of_week)
);

-- 3. User Shifts Assignment Table
CREATE TABLE wms.user_shifts (
    id                   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id              UUID NOT NULL REFERENCES wms.users(id) ON DELETE CASCADE,
    shift_id             UUID NOT NULL REFERENCES wms.wms_shifts(id) ON DELETE CASCADE,
    effective_start_date DATE NOT NULL,
    effective_end_date   DATE,
    created_at           TIMESTAMPTZ DEFAULT NOW(),
    created_by           VARCHAR(36) NOT NULL DEFAULT 'SYSTEM'
);

CREATE INDEX idx_user_shifts_user  ON wms.user_shifts(user_id);
CREATE INDEX idx_user_shifts_shift ON wms.user_shifts(shift_id);

-- 4. Register RBAC Permissions for Shifts
INSERT INTO wms.permissions (id, name, description)
VALUES 
    (uuid_generate_v4(), 'SHIFTS_READ',          'Permite ver catálogo y detalle de turnos'),
    (uuid_generate_v4(), 'SHIFTS_CREATE',        'Permite registrar nuevos turnos operativos'),
    (uuid_generate_v4(), 'SHIFTS_UPDATE',        'Permite actualizar turnos existentes'),
    (uuid_generate_v4(), 'SHIFTS_STATUS_CHANGE', 'Permite cambiar estatus ACTIVE/INACTIVE de turnos'),
    (uuid_generate_v4(), 'SHIFTS_DELETE',        'Permite realizar borrado lógico de turnos')
ON CONFLICT (name) DO NOTHING;

-- Map permissions to OPERATIONS_MANAGER (All permissions)
INSERT INTO wms.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM wms.roles r
CROSS JOIN wms.permissions p
WHERE r.name = 'OPERATIONS_MANAGER'
  AND p.name IN ('SHIFTS_READ', 'SHIFTS_CREATE', 'SHIFTS_UPDATE', 'SHIFTS_STATUS_CHANGE', 'SHIFTS_DELETE')
ON CONFLICT DO NOTHING;

-- Map read-only to CEO
INSERT INTO wms.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM wms.roles r
CROSS JOIN wms.permissions p
WHERE r.name = 'CEO'
  AND p.name IN ('SHIFTS_READ')
ON CONFLICT DO NOTHING;

-- Map read/update to OPERATIONS_SUPERVISOR and SHIFT_LEADER
INSERT INTO wms.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM wms.roles r
CROSS JOIN wms.permissions p
WHERE r.name IN ('OPERATIONS_SUPERVISOR', 'SHIFT_LEADER')
  AND p.name IN ('SHIFTS_READ', 'SHIFTS_UPDATE', 'SHIFTS_STATUS_CHANGE')
ON CONFLICT DO NOTHING;

-- 5. Seed 5 Dummy Test Shifts linked to CDMX HQ Branch (b73f0907-9fa5-4bdf-87db-2eb5e7683936)
-- Shift 1: Matutino Estándar (TRN-MAT-01)
INSERT INTO wms.wms_shifts (id, code, name, description, start_time, end_time, rest_break_minutes, tolerance_minutes, is_overnight, status, scope_type, branch_id, created_by)
VALUES (
    'f13f0907-9fa5-4bdf-87db-2eb5e7683951',
    'TRN-MAT-01',
    'Turno Matutino CDMX',
    'Jornada matutina estándar de almacén principal CDMX',
    '06:00:00',
    '14:00:00',
    30,
    10,
    FALSE,
    'ACTIVE',
    'BRANCH',
    'b73f0907-9fa5-4bdf-87db-2eb5e7683936',
    'enrique'
) ON CONFLICT DO NOTHING;

INSERT INTO wms.shift_operating_days (shift_id, day_of_week) VALUES
    ('f13f0907-9fa5-4bdf-87db-2eb5e7683951', 'MONDAY'),
    ('f13f0907-9fa5-4bdf-87db-2eb5e7683951', 'TUESDAY'),
    ('f13f0907-9fa5-4bdf-87db-2eb5e7683951', 'WEDNESDAY'),
    ('f13f0907-9fa5-4bdf-87db-2eb5e7683951', 'THURSDAY'),
    ('f13f0907-9fa5-4bdf-87db-2eb5e7683951', 'FRIDAY'),
    ('f13f0907-9fa5-4bdf-87db-2eb5e7683951', 'SATURDAY')
ON CONFLICT DO NOTHING;

-- Shift 2: Vespertino Estándar (TRN-VES-01)
INSERT INTO wms.wms_shifts (id, code, name, description, start_time, end_time, rest_break_minutes, tolerance_minutes, is_overnight, status, scope_type, branch_id, created_by)
VALUES (
    'f13f0907-9fa5-4bdf-87db-2eb5e7683952',
    'TRN-VES-01',
    'Turno Vespertino CDMX',
    'Jornada vespertina para surtido y recepción vespertina',
    '14:00:00',
    '22:00:00',
    45,
    15,
    FALSE,
    'ACTIVE',
    'BRANCH',
    'b73f0907-9fa5-4bdf-87db-2eb5e7683936',
    'enrique'
) ON CONFLICT DO NOTHING;

INSERT INTO wms.shift_operating_days (shift_id, day_of_week) VALUES
    ('f13f0907-9fa5-4bdf-87db-2eb5e7683952', 'MONDAY'),
    ('f13f0907-9fa5-4bdf-87db-2eb5e7683952', 'TUESDAY'),
    ('f13f0907-9fa5-4bdf-87db-2eb5e7683952', 'WEDNESDAY'),
    ('f13f0907-9fa5-4bdf-87db-2eb5e7683952', 'THURSDAY'),
    ('f13f0907-9fa5-4bdf-87db-2eb5e7683952', 'FRIDAY')
ON CONFLICT DO NOTHING;

-- Shift 3: Nocturno Overnight (TRN-NOC-01)
INSERT INTO wms.wms_shifts (id, code, name, description, start_time, end_time, rest_break_minutes, tolerance_minutes, is_overnight, status, scope_type, branch_id, created_by)
VALUES (
    'f13f0907-9fa5-4bdf-87db-2eb5e7683953',
    'TRN-NOC-01',
    'Turno Nocturno Overnight',
    'Jornada nocturna cruzando medianoche para acomodo e inventarios',
    '22:00:00',
    '06:00:00',
    60,
    10,
    TRUE,
    'ACTIVE',
    'BRANCH',
    'b73f0907-9fa5-4bdf-87db-2eb5e7683936',
    'enrique'
) ON CONFLICT DO NOTHING;

INSERT INTO wms.shift_operating_days (shift_id, day_of_week) VALUES
    ('f13f0907-9fa5-4bdf-87db-2eb5e7683953', 'MONDAY'),
    ('f13f0907-9fa5-4bdf-87db-2eb5e7683953', 'TUESDAY'),
    ('f13f0907-9fa5-4bdf-87db-2eb5e7683953', 'WEDNESDAY'),
    ('f13f0907-9fa5-4bdf-87db-2eb5e7683953', 'THURSDAY'),
    ('f13f0907-9fa5-4bdf-87db-2eb5e7683953', 'FRIDAY'),
    ('f13f0907-9fa5-4bdf-87db-2eb5e7683953', 'SATURDAY')
ON CONFLICT DO NOTHING;

-- Shift 4: Fin de Semana (TRN-MIX-01)
INSERT INTO wms.wms_shifts (id, code, name, description, start_time, end_time, rest_break_minutes, tolerance_minutes, is_overnight, status, scope_type, branch_id, created_by)
VALUES (
    'f13f0907-9fa5-4bdf-87db-2eb5e7683954',
    'TRN-MIX-01',
    'Turno Fines de Semana',
    'Jornada extendida sábado y domingo para guardia de embarques',
    '08:00:00',
    '20:00:00',
    60,
    15,
    FALSE,
    'ACTIVE',
    'BRANCH',
    'b73f0907-9fa5-4bdf-87db-2eb5e7683936',
    'enrique'
) ON CONFLICT DO NOTHING;

INSERT INTO wms.shift_operating_days (shift_id, day_of_week) VALUES
    ('f13f0907-9fa5-4bdf-87db-2eb5e7683954', 'SATURDAY'),
    ('f13f0907-9fa5-4bdf-87db-2eb5e7683954', 'SUNDAY')
ON CONFLICT DO NOTHING;

-- Shift 5: Especial Sección Picking (TRN-PICK-01)
INSERT INTO wms.wms_shifts (id, code, name, description, start_time, end_time, rest_break_minutes, tolerance_minutes, is_overnight, status, scope_type, branch_id, warehouse_section_id, created_by)
VALUES (
    'f13f0907-9fa5-4bdf-87db-2eb5e7683955',
    'TRN-PICK-01',
    'Turno Especial Picking Manual',
    'Turno focalizado en el área de picking manual (Sección SEC-PICK)',
    '07:00:00',
    '15:00:00',
    30,
    5,
    FALSE,
    'ACTIVE',
    'WAREHOUSE_SECTION',
    'b73f0907-9fa5-4bdf-87db-2eb5e7683936',
    'd13f0907-9fa5-4bdf-87db-2eb5e7683912',
    'enrique'
) ON CONFLICT DO NOTHING;

INSERT INTO wms.shift_operating_days (shift_id, day_of_week) VALUES
    ('f13f0907-9fa5-4bdf-87db-2eb5e7683955', 'MONDAY'),
    ('f13f0907-9fa5-4bdf-87db-2eb5e7683955', 'TUESDAY'),
    ('f13f0907-9fa5-4bdf-87db-2eb5e7683955', 'WEDNESDAY'),
    ('f13f0907-9fa5-4bdf-87db-2eb5e7683955', 'THURSDAY'),
    ('f13f0907-9fa5-4bdf-87db-2eb5e7683955', 'FRIDAY')
ON CONFLICT DO NOTHING;

-- 6. Seed User Shift Allocations for Test Users (enrique, Chris4G, Romel4G)
INSERT INTO wms.user_shifts (id, user_id, shift_id, effective_start_date, created_by)
VALUES
    ('a13f0907-9fa5-4bdf-87db-2eb5e7683901', 'f33f0907-9fa5-4bdf-87db-2eb5e7683937', 'f13f0907-9fa5-4bdf-87db-2eb5e7683951', '2026-01-01', 'enrique'),
    ('a13f0907-9fa5-4bdf-87db-2eb5e7683902', 'afe4de7c-d10e-44b9-8970-46a0fda50626', 'f13f0907-9fa5-4bdf-87db-2eb5e7683952', '2026-01-01', 'enrique'),
    ('a13f0907-9fa5-4bdf-87db-2eb5e7683903', 'fb31fe4c-bc27-4b1c-8846-7288812f84bf', 'f13f0907-9fa5-4bdf-87db-2eb5e7683953', '2026-01-01', 'enrique')
ON CONFLICT DO NOTHING;
