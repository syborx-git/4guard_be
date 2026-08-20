-- =============================================================================
-- V6__forklift_operators_schema.sql
-- Gestión de Montacarguistas (HU-142) — 4GUARD WMS
-- Crea la tabla wms.forklift_operators con índices, permisos y asignación
-- a roles. FK hacia wms.organizations, wms.branches y wms.wms_shifts.
-- =============================================================================

-- ---------------------------------------------------------------------------
-- 1. TABLA MAESTRA DE MONTACARGUISTAS
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS wms.forklift_operators (
    id                      UUID         NOT NULL DEFAULT uuid_generate_v4(),
    organization_id         UUID         NOT NULL,
    branch_id               UUID,
    code                    VARCHAR(30)  NOT NULL,
    first_name              VARCHAR(100) NOT NULL,
    last_name_paternal      VARCHAR(100) NOT NULL,
    last_name_maternal      VARCHAR(100) NOT NULL,
    full_name               VARCHAR(310) NOT NULL,
    license_number_dc3      VARCHAR(50)  NOT NULL,
    license_expiration_date DATE         NOT NULL,
    license_status          VARCHAR(20)  NOT NULL DEFAULT 'VIGENTE',
    shift_id                UUID,
    shift_name              VARCHAR(150),
    status                  VARCHAR(20)  NOT NULL DEFAULT 'ACTIVO',
    is_deleted              BOOLEAN      NOT NULL DEFAULT FALSE,
    version                 BIGINT       NOT NULL DEFAULT 1,
    created_at              TIMESTAMPTZ           DEFAULT NOW(),
    updated_at              TIMESTAMPTZ           DEFAULT NOW(),
    created_by              VARCHAR(36),
    updated_by              VARCHAR(36),

    CONSTRAINT pk_forklift_operators          PRIMARY KEY (id),
    CONSTRAINT fk_forklift_op_organization    FOREIGN KEY (organization_id) REFERENCES wms.organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_forklift_op_branch          FOREIGN KEY (branch_id)       REFERENCES wms.branches(id)       ON DELETE SET NULL,
    CONSTRAINT fk_forklift_op_shift           FOREIGN KEY (shift_id)        REFERENCES wms.wms_shifts(id)     ON DELETE SET NULL,
    CONSTRAINT uk_forklift_operator_code      UNIQUE (organization_id, code),
    CONSTRAINT uk_forklift_operator_dc3       UNIQUE (organization_id, license_number_dc3),
    CONSTRAINT chk_forklift_op_status         CHECK (status IN ('ACTIVO', 'INACTIVO')),
    CONSTRAINT chk_forklift_op_license_status CHECK (license_status IN ('VIGENTE', 'POR_VENCER', 'VENCIDA'))
);

COMMENT ON TABLE  wms.forklift_operators                    IS 'Catálogo de operadores de montacargas certificados DC-3 (HU-142). No son usuarios del sistema.';
COMMENT ON COLUMN wms.forklift_operators.code               IS 'Consecutivo operativo generado por el sistema, ej. MC-001.';
COMMENT ON COLUMN wms.forklift_operators.license_number_dc3 IS 'Número de certificación registrado ante STPS / constancia DC-3.';
COMMENT ON COLUMN wms.forklift_operators.license_status     IS 'Calculado automáticamente: VIGENTE (>30 días), POR_VENCER (<=30 días), VENCIDA.';
COMMENT ON COLUMN wms.forklift_operators.shift_id           IS 'Turno asignado del catálogo maestro wms.wms_shifts.';

-- ---------------------------------------------------------------------------
-- 2. ÍNDICES DE RENDIMIENTO
-- ---------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_forklift_op_org
    ON wms.forklift_operators (organization_id)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_forklift_op_org_status
    ON wms.forklift_operators (organization_id, status)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_forklift_op_shift
    ON wms.forklift_operators (shift_id);

CREATE INDEX IF NOT EXISTS idx_forklift_op_lic_status
    ON wms.forklift_operators (organization_id, license_status)
    WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_forklift_op_expiration
    ON wms.forklift_operators (license_expiration_date)
    WHERE is_deleted = FALSE;

-- ---------------------------------------------------------------------------
-- 3. PERMISOS ESPECÍFICOS DEL MÓDULO
-- ---------------------------------------------------------------------------
INSERT INTO wms.permissions (id, name, description)
VALUES
    (uuid_generate_v4(), 'FORKLIFT_OPERATORS_READ',          'Permite consultar el catálogo de montacarguistas'),
    (uuid_generate_v4(), 'FORKLIFT_OPERATORS_CREATE',        'Permite registrar nuevos montacarguistas'),
    (uuid_generate_v4(), 'FORKLIFT_OPERATORS_UPDATE',        'Permite modificar datos de montacarguistas existentes'),
    (uuid_generate_v4(), 'FORKLIFT_OPERATORS_DELETE',        'Permite dar de baja definitiva a montacarguistas'),
    (uuid_generate_v4(), 'FORKLIFT_OPERATORS_STATUS_CHANGE', 'Permite cambiar el estatus ACTIVO/INACTIVO de un montacarguista')
ON CONFLICT (name) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 4. ASIGNACIÓN DE PERMISOS A ROLES ADMINISTRATIVOS Y OPERATIVOS
-- ---------------------------------------------------------------------------
INSERT INTO wms.role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM wms.roles r
CROSS JOIN wms.permissions p
WHERE r.name IN ('SUPER_ADMIN', 'OPERATIONS_MANAGER', 'WAREHOUSE_SUPERVISOR', 'SHIFT_LEADER')
  AND p.name LIKE 'FORKLIFT_OPERATORS_%'
ON CONFLICT DO NOTHING;
