-- =============================================================================
-- V1: Consolidated Master Schema
-- Author: 4GUARD Engineering Team (Senior Architect)
-- Description: Master consolidated DDL script creating all database tables,
--              foreign key relationships, triggers, functions, and performance
--              indexes for 4Guard WMS (RBAC, Topology, Inventory, Quality,
--              Audit, Carriers, Suppliers, Shifts, and Alert Configurations).
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS wms;
SET search_path TO wms, public;

-- Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- =============================================================================
-- SECTION 1: CORE IDENTITY AND SECURITY (RBAC)
-- =============================================================================

CREATE TABLE wms.organizations (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name            VARCHAR(200) NOT NULL,
    code            VARCHAR(20)  UNIQUE NOT NULL,
    tax_id          VARCHAR(20)  UNIQUE,
    type            VARCHAR(50)  NOT NULL,
    status          VARCHAR(20)  DEFAULT 'ACTIVE',
    settings        JSONB        DEFAULT '{}',
    version         BIGINT       DEFAULT 1,
    created_at      TIMESTAMPTZ  DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  DEFAULT NOW(),
    created_by      VARCHAR(36),
    updated_by      VARCHAR(36)
);

CREATE TABLE wms.roles (
    id          UUID    PRIMARY KEY DEFAULT uuid_generate_v4(),
    name        VARCHAR(50) UNIQUE NOT NULL,
    level       INTEGER NOT NULL CHECK (level BETWEEN 1 AND 7),
    is_system   BOOLEAN DEFAULT FALSE,
    version     BIGINT  DEFAULT 1,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    updated_at  TIMESTAMPTZ DEFAULT NOW(),
    created_by  VARCHAR(36),
    updated_by  VARCHAR(36)
);

CREATE TABLE wms.permissions (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name        VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE wms.role_permissions (
    role_id       UUID NOT NULL REFERENCES wms.roles(id)       ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES wms.permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE wms.branches (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID        NOT NULL REFERENCES wms.organizations(id),
    name            VARCHAR(200) NOT NULL,
    code            VARCHAR(20)  NOT NULL,
    timezone        VARCHAR(50)  DEFAULT 'UTC',
    address_line1   TEXT,
    status          VARCHAR(20)  DEFAULT 'ACTIVE',
    version         BIGINT       DEFAULT 1,
    created_at      TIMESTAMPTZ  DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  DEFAULT NOW(),
    created_by      VARCHAR(36),
    updated_by      VARCHAR(36),
    UNIQUE(organization_id, code)
);

CREATE TABLE wms.users (
    id                       UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username                 VARCHAR(50)  UNIQUE NOT NULL,
    email                    VARCHAR(255) UNIQUE NOT NULL,
    password                 VARCHAR(255) NOT NULL,
    first_name               VARCHAR(100),
    last_name                VARCHAR(100),
    organization_id          UUID         NOT NULL REFERENCES wms.organizations(id),
    branch_id                UUID                  REFERENCES wms.branches(id),
    role_id                  UUID         NOT NULL REFERENCES wms.roles(id),
    status                   VARCHAR(20)  DEFAULT 'PENDING',
    is_enabled               BOOLEAN      NOT NULL DEFAULT FALSE,
    change_password_required BOOLEAN      NOT NULL DEFAULT TRUE,
    failed_attempts          INTEGER      NOT NULL DEFAULT 0,
    locked_until             TIMESTAMPTZ,
    permanently_locked       BOOLEAN      NOT NULL DEFAULT FALSE,
    last_login               TIMESTAMPTZ,
    version                  BIGINT       DEFAULT 1,
    created_at               TIMESTAMPTZ  DEFAULT NOW(),
    updated_at               TIMESTAMPTZ  DEFAULT NOW(),
    created_by               VARCHAR(36),
    updated_by               VARCHAR(36)
);

-- =============================================================================
-- SECTION 2: WAREHOUSE TOPOLOGY
-- =============================================================================

CREATE TABLE wms.warehouse_sections (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    branch_id   UUID        NOT NULL REFERENCES wms.branches(id),
    code        VARCHAR(10) NOT NULL,
    name        VARCHAR(100),
    status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    is_active   BOOLEAN     NOT NULL DEFAULT TRUE,
    is_deleted  BOOLEAN     NOT NULL DEFAULT FALSE,
    version     BIGINT      DEFAULT 1,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    updated_at  TIMESTAMPTZ DEFAULT NOW(),
    created_by  VARCHAR(36),
    updated_by  VARCHAR(36)
);

CREATE TABLE wms.locations (
    id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    branch_id         UUID        NOT NULL REFERENCES wms.branches(id),
    section_id        UUID                 REFERENCES wms.warehouse_sections(id) ON DELETE CASCADE,
    code              VARCHAR(30) UNIQUE,
    name              VARCHAR(150),
    zone              VARCHAR(10) NOT NULL,
    aisle             VARCHAR(10),
    rack              VARCHAR(10),
    level             INTEGER,
    position          VARCHAR(10),
    coord_x           INTEGER,
    coord_y           INTEGER,
    coord_z           INTEGER,
    type              VARCHAR(20) CHECK (type IN ('PALLET', 'BIN', 'SHELF', 'RAMP')),
    status            VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    status_reason     VARCHAR(300),
    is_active         BOOLEAN     NOT NULL DEFAULT TRUE,
    is_deleted        BOOLEAN     NOT NULL DEFAULT FALSE,
    notes             TEXT,
    capacity_units    INTEGER     DEFAULT 1,
    current_occupancy INTEGER     DEFAULT 0,
    is_blocked        BOOLEAN     DEFAULT FALSE,
    block_reason      TEXT,
    version           BIGINT      DEFAULT 1,
    created_at        TIMESTAMPTZ DEFAULT NOW(),
    updated_at        TIMESTAMPTZ DEFAULT NOW(),
    created_by        VARCHAR(36),
    updated_by        VARCHAR(36)
);

CREATE TABLE wms.notifications (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES wms.organizations(id) ON DELETE CASCADE,
    recipient_id    UUID REFERENCES wms.users(id) ON DELETE CASCADE,
    type            VARCHAR(50) NOT NULL,
    title           VARCHAR(200) NOT NULL,
    message         TEXT NOT NULL,
    is_read         BOOLEAN NOT NULL DEFAULT FALSE,
    metadata        JSONB DEFAULT '{}',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE wms.organization_settings (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID NOT NULL REFERENCES wms.organizations(id) ON DELETE CASCADE,
    setting_key     VARCHAR(100) NOT NULL,
    setting_value   TEXT,
    CONSTRAINT uk_org_setting_key UNIQUE (organization_id, setting_key)
);

-- =============================================================================
-- SECTION 3: MERCHANDISE AND INVENTORY
-- =============================================================================

CREATE TABLE wms.clients (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID         NOT NULL REFERENCES wms.organizations(id),
    name            VARCHAR(200) NOT NULL,
    external_id     VARCHAR(50),
    tax_id          VARCHAR(30),
    status          VARCHAR(20)  DEFAULT 'ACTIVE',
    version         BIGINT       DEFAULT 1,
    created_at      TIMESTAMPTZ  DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  DEFAULT NOW(),
    created_by      VARCHAR(36),
    updated_by      VARCHAR(36)
);

CREATE TABLE wms.products_sku (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    client_id   UUID            NOT NULL REFERENCES wms.clients(id),
    code        VARCHAR(50)     NOT NULL,
    name        VARCHAR(200)    NOT NULL,
    description TEXT,
    weight      DECIMAL(10,3),
    unit        VARCHAR(20)     NOT NULL,
    version     BIGINT          DEFAULT 1,
    created_at  TIMESTAMPTZ     DEFAULT NOW(),
    updated_at  TIMESTAMPTZ     DEFAULT NOW(),
    created_by  VARCHAR(36),
    updated_by  VARCHAR(36)
);

CREATE TABLE wms.inventory_items (
    id                   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id      UUID            NOT NULL REFERENCES wms.organizations(id),
    branch_id            UUID            NOT NULL REFERENCES wms.branches(id),
    client_id            UUID            NOT NULL REFERENCES wms.clients(id),
    sscc                 VARCHAR(20)     UNIQUE NOT NULL,
    external_ua          VARCHAR(20),
    sku_id               UUID            NOT NULL REFERENCES wms.products_sku(id),
    location_id          UUID                     REFERENCES wms.locations(id),
    state                INTEGER         NOT NULL CHECK (state IN (10, 20, 30, 40, 50, 60, 70, 80)),
    quantity             DECIMAL(12,3)   NOT NULL DEFAULT 0,
    batch_number         VARCHAR(50),
    manufacturing_date   DATE,
    expiration_date      DATE,
    sap_folio            VARCHAR(50),
    quarantine_reason    TEXT,
    metadata             JSONB           DEFAULT '{}',
    version              BIGINT          NOT NULL DEFAULT 1,
    created_at           TIMESTAMPTZ     DEFAULT NOW(),
    updated_at           TIMESTAMPTZ     DEFAULT NOW(),
    created_by           VARCHAR(36),
    updated_by           VARCHAR(36)
);

CREATE TABLE wms.inventory_movements (
    id               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    item_id          UUID        NOT NULL REFERENCES wms.inventory_items(id),
    from_location_id UUID                  REFERENCES wms.locations(id),
    to_location_id   UUID                  REFERENCES wms.locations(id),
    user_id          UUID        NOT NULL REFERENCES wms.users(id),
    type             VARCHAR(50) NOT NULL,
    reason           TEXT,
    created_at       TIMESTAMPTZ DEFAULT NOW()
);

-- =============================================================================
-- SECTION 4: QUALITY AND AUDIT
-- =============================================================================

CREATE TABLE wms.incidences (
    id             UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    folio          SERIAL UNIQUE,
    item_id        UUID        NOT NULL REFERENCES wms.inventory_items(id),
    type           VARCHAR(50) NOT NULL,
    severity       VARCHAR(20) CHECK (severity IN ('RED', 'YELLOW', 'BLUE')),
    reported_by_id UUID                  REFERENCES wms.users(id),
    status         VARCHAR(20) DEFAULT 'OPEN',
    created_at     TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE wms.audit_logs (
    log_id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id UUID         NOT NULL REFERENCES wms.organizations(id),
    branch_id       UUID                   REFERENCES wms.branches(id),
    user_id         UUID         NOT NULL REFERENCES wms.users(id),
    action          VARCHAR(100) NOT NULL,
    entity_type     VARCHAR(50)  NOT NULL,
    entity_id       UUID         NOT NULL,
    ip_address      VARCHAR(45),
    user_agent      TEXT,
    created_at      TIMESTAMPTZ  DEFAULT NOW()
);

CREATE TABLE wms.audit_log_details (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    log_id      UUID NOT NULL REFERENCES wms.audit_logs(log_id) ON DELETE CASCADE,
    field_name  VARCHAR(100) NOT NULL,
    old_value   TEXT,
    new_value   TEXT
);

-- =============================================================================
-- SECTION 5: CARRIERS MANAGEMENT (TRANSPORTISTAS)
-- =============================================================================

CREATE TABLE wms.carriers (
    id                   UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id      UUID         NOT NULL REFERENCES wms.organizations(id),
    name                 VARCHAR(200) NOT NULL,
    trade_name           VARCHAR(200) NOT NULL,
    tax_id               VARCHAR(30)  NOT NULL,
    carrier_type         VARCHAR(50)  NOT NULL DEFAULT 'EXTERNAL',
    status               VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    contact_name         VARCHAR(150) NOT NULL,
    contact_phone        VARCHAR(20)  NOT NULL,
    contact_email        VARCHAR(255) NOT NULL,
    service_type         VARCHAR(50)  NOT NULL DEFAULT 'FTL',
    permit_number        VARCHAR(100),
    geographic_coverage  TEXT,
    notes                TEXT,
    version              BIGINT       NOT NULL DEFAULT 1,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by           VARCHAR(36),
    updated_by           VARCHAR(36),
    UNIQUE (organization_id, name),
    CONSTRAINT chk_carrier_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED')),
    CONSTRAINT chk_carrier_type   CHECK (carrier_type IN ('EXTERNAL', 'CLIENT_TRANSPORT', 'OWN_TRANSPORT', 'THIRD_PARTY_3PL', 'PARCEL')),
    CONSTRAINT chk_service_type   CHECK (service_type IN ('FTL', 'LTL', 'PARCEL', 'INTERMODAL', 'LAST_MILE', 'DEDICATED'))
);

CREATE TABLE wms.carrier_vehicle_types (
    carrier_id   UUID        NOT NULL REFERENCES wms.carriers(id) ON DELETE CASCADE,
    vehicle_type VARCHAR(50) NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (carrier_id, vehicle_type)
);

CREATE TABLE wms.carrier_preferred_clients (
    carrier_id   UUID        NOT NULL REFERENCES wms.carriers(id) ON DELETE CASCADE,
    client_id    UUID        NOT NULL REFERENCES wms.clients(id) ON DELETE CASCADE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (carrier_id, client_id)
);

CREATE TABLE wms.carrier_vehicle_metadata (
    id              UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    item_id         UUID         NOT NULL REFERENCES wms.inventory_items(id) ON DELETE CASCADE,
    carrier_id      UUID         REFERENCES wms.carriers(id) ON DELETE SET NULL,
    vehicle_plates  VARCHAR(20)  NOT NULL,
    driver_name     VARCHAR(150),
    driver_license  VARCHAR(30),
    seal_count      INTEGER      DEFAULT 0,
    operation_type  VARCHAR(20)  NOT NULL DEFAULT 'RECEIVING',
    registered_by   UUID         NOT NULL REFERENCES wms.users(id),
    registered_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    notes           TEXT,
    version         BIGINT       NOT NULL DEFAULT 1,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(36),
    updated_by      VARCHAR(36)
);

CREATE TABLE wms.vehicle_seals (
    id              UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    metadata_id     UUID        NOT NULL REFERENCES wms.carrier_vehicle_metadata(id) ON DELETE CASCADE,
    seal_number     VARCHAR(50) NOT NULL,
    seal_type       VARCHAR(30) DEFAULT 'STANDARD',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(36)
);

-- =============================================================================
-- SECTION 6: SUPPLIERS MANAGEMENT (PROVEEDORES)
-- =============================================================================

CREATE TABLE wms.cat_supplier_types (
    code        VARCHAR(30)     PRIMARY KEY,
    label_es    VARCHAR(100)    NOT NULL,
    label_en    VARCHAR(100),
    is_service  BOOLEAN         NOT NULL DEFAULT FALSE,
    sort_order  SMALLINT        NOT NULL DEFAULT 0,
    active      BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE TABLE wms.cat_currencies (
    code    CHAR(3)     PRIMARY KEY,
    label   VARCHAR(60) NOT NULL,
    symbol  VARCHAR(5),
    active  BOOLEAN     NOT NULL DEFAULT TRUE
);

CREATE TABLE wms.suppliers (
    id                  UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id     UUID            NOT NULL REFERENCES wms.organizations(id),
    code                VARCHAR(20)     NOT NULL,
    legal_name          VARCHAR(250)    NOT NULL,
    commercial_name     VARCHAR(150),
    tax_id              VARCHAR(20)     NOT NULL,
    supplier_type_code  VARCHAR(30)     NOT NULL REFERENCES wms.cat_supplier_types(code),
    is_preferred        BOOLEAN         NOT NULL DEFAULT FALSE,
    status              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    status_reason       VARCHAR(500),
    status_changed_at   TIMESTAMPTZ,
    status_changed_by   VARCHAR(100),
    scope_type          VARCHAR(15)     NOT NULL DEFAULT 'GLOBAL',
    client_id           UUID            REFERENCES wms.clients(id),
    branch_id           UUID            REFERENCES wms.branches(id),
    notes               TEXT,
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    deleted_at          TIMESTAMPTZ,
    deleted_by          VARCHAR(100),
    version             BIGINT          NOT NULL DEFAULT 1,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    created_by          VARCHAR(100)    NOT NULL DEFAULT 'system',
    updated_by          VARCHAR(100)    NOT NULL DEFAULT 'system',
    UNIQUE (organization_id, code),
    UNIQUE (organization_id, tax_id)
);

CREATE TABLE wms.supplier_contacts (
    id              UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
    supplier_id     UUID            NOT NULL UNIQUE REFERENCES wms.suppliers(id) ON DELETE CASCADE,
    full_name       VARCHAR(150)    NOT NULL,
    job_title       VARCHAR(100),
    email           VARCHAR(150)    NOT NULL,
    phone           VARCHAR(25)     NOT NULL,
    alt_phone       VARCHAR(25),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE TABLE wms.supplier_addresses (
    id                  UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
    supplier_id         UUID            NOT NULL UNIQUE REFERENCES wms.suppliers(id) ON DELETE CASCADE,
    country             VARCHAR(80)     NOT NULL DEFAULT 'México',
    state               VARCHAR(80)     NOT NULL,
    municipality        VARCHAR(80),
    city                VARCHAR(80)     NOT NULL,
    postal_code         VARCHAR(10),
    street              VARCHAR(200),
    exterior_number     VARCHAR(20),
    interior_number     VARCHAR(20),
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE TABLE wms.supplier_commercial_terms (
    id                              UUID            PRIMARY KEY DEFAULT uuid_generate_v4(),
    supplier_id                     UUID            NOT NULL UNIQUE REFERENCES wms.suppliers(id) ON DELETE CASCADE,
    lead_time_days                  SMALLINT        NOT NULL DEFAULT 0,
    minimum_order_amount            NUMERIC(14,2)   NOT NULL DEFAULT 0,
    credit_days                     SMALLINT        NOT NULL DEFAULT 0,
    currency_code                   CHAR(3)         NOT NULL DEFAULT 'MXN' REFERENCES wms.cat_currencies(code),
    quality_inspection_required     BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at                      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at                      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- =============================================================================
-- SECTION 7: SHIFTS AND SCHEDULES (TURNOS)
-- =============================================================================

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

CREATE TABLE wms.shift_operating_days (
    shift_id    UUID        NOT NULL REFERENCES wms.wms_shifts(id) ON DELETE CASCADE,
    day_of_week VARCHAR(15) NOT NULL,
    PRIMARY KEY (shift_id, day_of_week)
);

CREATE TABLE wms.user_shifts (
    id                   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id              UUID NOT NULL REFERENCES wms.users(id) ON DELETE CASCADE,
    shift_id             UUID NOT NULL REFERENCES wms.wms_shifts(id) ON DELETE CASCADE,
    effective_start_date DATE NOT NULL,
    effective_end_date   DATE,
    created_at           TIMESTAMPTZ DEFAULT NOW(),
    created_by           VARCHAR(36) NOT NULL DEFAULT 'SYSTEM'
);

-- =============================================================================
-- SECTION 8: NOTIFICATION & ALERT CONFIGURATIONS (ALERTAS HU-134)
-- =============================================================================

CREATE TABLE wms.alert_configurations (
    id                   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id      UUID NOT NULL REFERENCES wms.organizations(id) ON DELETE CASCADE,
    name                 VARCHAR(255) NOT NULL,
    category             VARCHAR(50)  NOT NULL,
    event                VARCHAR(100) NOT NULL,
    priority             VARCHAR(20)  NOT NULL,
    status               VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    channels             TEXT[]       NOT NULL,
    recipients           TEXT[]       NOT NULL,
    condition            VARCHAR(50)  NOT NULL,
    value                NUMERIC(12, 2) NOT NULL,
    unit                 VARCHAR(30)  NOT NULL,
    recurrence           VARCHAR(30)  NOT NULL DEFAULT 'NEVER',
    escalation           VARCHAR(30)  NOT NULL DEFAULT 'NONE',
    message_template     TEXT         NOT NULL,
    description          TEXT,
    is_deleted           BOOLEAN      NOT NULL DEFAULT FALSE,
    version              BIGINT       DEFAULT 1,
    created_at           TIMESTAMPTZ  DEFAULT NOW(),
    updated_at           TIMESTAMPTZ  DEFAULT NOW(),
    created_by           VARCHAR(255) NOT NULL DEFAULT 'SYSTEM',
    updated_by           VARCHAR(255),
    deleted_at           TIMESTAMPTZ,
    CONSTRAINT uk_alert_name_org UNIQUE (organization_id, name)
);

CREATE TABLE wms.alert_fired_events (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    alert_configuration_id UUID NOT NULL REFERENCES wms.alert_configurations(id) ON DELETE CASCADE,
    organization_id         UUID NOT NULL REFERENCES wms.organizations(id),
    branch_id               UUID REFERENCES wms.branches(id),
    triggered_at            TIMESTAMPTZ DEFAULT NOW(),
    entity_reference        VARCHAR(255),
    evaluated_value         NUMERIC(12, 2),
    status                  VARCHAR(30) DEFAULT 'FIRED',
    acknowledged_by         VARCHAR(255),
    acknowledged_at         TIMESTAMPTZ
);

-- =============================================================================
-- SECTION 9: TRIGGERS & PROCEDURES
-- =============================================================================

CREATE OR REPLACE FUNCTION wms.protect_audit_logs()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Operation not permitted: this table is immutable (WORM)';
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION wms.update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    NEW.version    = OLD.version + 1;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_inventory
    BEFORE UPDATE ON wms.inventory_items
    FOR EACH ROW EXECUTE FUNCTION wms.update_updated_at_column();

CREATE TRIGGER trg_update_users
    BEFORE UPDATE ON wms.users
    FOR EACH ROW EXECUTE FUNCTION wms.update_updated_at_column();

CREATE TRIGGER trg_audit_logs_worm
    BEFORE UPDATE OR DELETE ON wms.audit_logs
    FOR EACH ROW EXECUTE FUNCTION wms.protect_audit_logs();

CREATE TRIGGER trg_update_carriers
    BEFORE UPDATE ON wms.carriers
    FOR EACH ROW EXECUTE FUNCTION wms.update_updated_at_column();

CREATE TRIGGER trg_update_carrier_vehicle_metadata
    BEFORE UPDATE ON wms.carrier_vehicle_metadata
    FOR EACH ROW EXECUTE FUNCTION wms.update_updated_at_column();

CREATE TRIGGER trg_update_suppliers
    BEFORE UPDATE ON wms.suppliers
    FOR EACH ROW EXECUTE FUNCTION wms.update_updated_at_column();

CREATE TRIGGER trg_update_supplier_contacts
    BEFORE UPDATE ON wms.supplier_contacts
    FOR EACH ROW EXECUTE FUNCTION wms.update_updated_at_column();

CREATE TRIGGER trg_update_supplier_addresses
    BEFORE UPDATE ON wms.supplier_addresses
    FOR EACH ROW EXECUTE FUNCTION wms.update_updated_at_column();

CREATE TRIGGER trg_update_supplier_commercial_terms
    BEFORE UPDATE ON wms.supplier_commercial_terms
    FOR EACH ROW EXECUTE FUNCTION wms.update_updated_at_column();

-- =============================================================================
-- SECTION 10: PERFORMANCE INDEXES
-- =============================================================================

CREATE INDEX idx_inventory_sscc       ON wms.inventory_items (sscc);
CREATE INDEX idx_inventory_fefo       ON wms.inventory_items (sku_id, expiration_date) WHERE state = 30;
CREATE INDEX idx_audit_created        ON wms.audit_logs (created_at DESC);
CREATE INDEX idx_carriers_org         ON wms.carriers (organization_id);
CREATE INDEX idx_cvt_carrier          ON wms.carrier_vehicle_types (carrier_id);
CREATE INDEX idx_cpc_carrier          ON wms.carrier_preferred_clients (carrier_id);
CREATE INDEX idx_cvm_item_id          ON wms.carrier_vehicle_metadata (item_id);
CREATE INDEX idx_seals_metadata_id    ON wms.vehicle_seals (metadata_id);
CREATE INDEX idx_suppliers_org        ON wms.suppliers (organization_id, is_deleted);
CREATE INDEX idx_suppliers_status     ON wms.suppliers (status, is_deleted);
CREATE INDEX idx_suppliers_type       ON wms.suppliers (supplier_type_code);
CREATE INDEX idx_suppliers_scope      ON wms.suppliers (scope_type, organization_id);
CREATE INDEX idx_suppliers_legal_name ON wms.suppliers (lower(legal_name));
CREATE INDEX idx_suppliers_tax_id     ON wms.suppliers (tax_id);
CREATE INDEX idx_suppliers_preferred  ON wms.suppliers (is_preferred, is_deleted);
CREATE INDEX idx_wms_shifts_branch     ON wms.wms_shifts(branch_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_wms_shifts_status     ON wms.wms_shifts(status) WHERE is_deleted = FALSE;
CREATE INDEX idx_wms_shifts_scope      ON wms.wms_shifts(scope_type, branch_id) WHERE is_deleted = FALSE;
CREATE INDEX idx_user_shifts_user     ON wms.user_shifts(user_id);
CREATE INDEX idx_user_shifts_shift    ON wms.user_shifts(shift_id);
CREATE INDEX idx_alert_config_org_status ON wms.alert_configurations(organization_id, status) WHERE is_deleted = FALSE;
CREATE INDEX idx_alert_config_event      ON wms.alert_configurations(event) WHERE is_deleted = FALSE;
CREATE INDEX idx_alert_fired_org_status ON wms.alert_fired_events(organization_id, status);
CREATE INDEX idx_alert_fired_config_id  ON wms.alert_fired_events(alert_configuration_id);
CREATE INDEX idx_alert_fired_triggered  ON wms.alert_fired_events(triggered_at DESC);
