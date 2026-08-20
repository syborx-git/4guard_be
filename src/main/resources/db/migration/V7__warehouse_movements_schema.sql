-- =============================================================================
-- V7: Warehouse Movements Schema
-- Author: 4GUARD Engineering Team
-- Description: DDL for Warehouse Movements module — Receiving (F01),
--              Transfers (Cambio de Almacén) and Outbound Dispatches (F03).
--              Adds 7 transactional tables + sequences + performance indexes.
--              No existing tables are modified.
-- =============================================================================

SET search_path TO wms, public;

-- =============================================================================
-- SEQUENCES: Consecutive folio generation (thread-safe under concurrency)
-- =============================================================================

CREATE SEQUENCE IF NOT EXISTS wms.seq_reception_folio
    START WITH 26510
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS wms.seq_transfer_folio
    START WITH 4082
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE IF NOT EXISTS wms.seq_outbound_folio
    START WITH 2
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

-- =============================================================================
-- TABLE 1: wms.warehouse_receptions
-- Reception header (Caseta check-in + Andén parameters)
-- =============================================================================

CREATE TABLE wms.warehouse_receptions (
    id                      UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id         UUID         NOT NULL REFERENCES wms.organizations(id),
    branch_id               UUID         NOT NULL REFERENCES wms.branches(id),

    -- Consecutive folio: numeric for reception (e.g. 26510)
    folio                   VARCHAR(30)  NOT NULL UNIQUE,

    status                  VARCHAR(20)  NOT NULL DEFAULT 'REGISTERED'
                                CONSTRAINT chk_wr_status CHECK (status IN ('REGISTERED', 'COMPLETED', 'CANCELLED')),

    -- ── Caseta Check-In Data ─────────────────────────────────────────────────
    carrier_id              UUID                  REFERENCES wms.carriers(id),
    client_id               UUID         NOT NULL REFERENCES wms.clients(id),
    ramp_id                 UUID                  REFERENCES wms.locations(id),
    forklift_operator_id    UUID                  REFERENCES wms.forklift_operators(id),
    doc_number              VARCHAR(60)  NOT NULL,
    doc_date                DATE         NOT NULL,
    reception_time          TIME         NOT NULL,
    driver_name             VARCHAR(150) NOT NULL,
    tractor_plates          VARCHAR(20)  NOT NULL,
    box_plates              VARCHAR(20)  NOT NULL,

    -- ── Andén / Lot Parameters ───────────────────────────────────────────────
    sku_id                  UUID                  REFERENCES wms.products_sku(id),
    supplier_id             UUID                  REFERENCES wms.suppliers(id),
    lot_number              VARCHAR(50),
    elaboration_date        DATE,
    expiration_date         DATE,
    pieces_per_pallet       NUMERIC(10,2)         DEFAULT 0,
    pallet_type             VARCHAR(30)           DEFAULT 'MADERA_ESTANDAR'
                                CONSTRAINT chk_wr_pallet_type CHECK (pallet_type IN (
                                    'MADERA_ESTANDAR','TARIMA_CHEP','PLASTICO',
                                    'PLASTICO_AZUL','MADERA_EXPORTACION','SIN_TARIMA','MADERA'
                                )),
    storage_location_id     UUID                  REFERENCES wms.locations(id),
    observations            TEXT,

    -- ── Completion / Cancellation Data ───────────────────────────────────────
    completed_at            TIMESTAMPTZ,
    leader_authorized_by    VARCHAR(100),
    cancelled_at            TIMESTAMPTZ,
    cancellation_reason     TEXT,
    cancelled_by            VARCHAR(100),

    -- ── Audit + Optimistic Locking ───────────────────────────────────────────
    version                 BIGINT       NOT NULL DEFAULT 1,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by              VARCHAR(36),
    updated_by              VARCHAR(36)
);

-- =============================================================================
-- TABLE 2: wms.warehouse_reception_pallets
-- Individual pallet/UA detail for each reception
-- =============================================================================

CREATE TABLE wms.warehouse_reception_pallets (
    id                  UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    reception_id        UUID         NOT NULL REFERENCES wms.warehouse_receptions(id),
    pallet_number       INTEGER      NOT NULL DEFAULT 1,

    -- UA / SSCC barcode — unique within the same reception
    pallet_code         VARCHAR(50)  NOT NULL,

    sku_id              UUID         NOT NULL REFERENCES wms.products_sku(id),
    supplier_id         UUID                  REFERENCES wms.suppliers(id),
    pieces              NUMERIC(10,2) NOT NULL DEFAULT 0,
    pallet_type         VARCHAR(30)  NOT NULL DEFAULT 'MADERA_ESTANDAR'
                            CONSTRAINT chk_wrp_pallet_type CHECK (pallet_type IN (
                                'MADERA_ESTANDAR','TARIMA_CHEP','PLASTICO',
                                'PLASTICO_AZUL','MADERA_EXPORTACION','SIN_TARIMA','MADERA'
                            )),
    observations        TEXT,

    -- Link to inventory_items once reception is COMPLETED
    inventory_item_id   UUID                  REFERENCES wms.inventory_items(id),

    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_reception_pallet_code UNIQUE (reception_id, pallet_code)
);

-- =============================================================================
-- TABLE 3: wms.warehouse_reception_seals
-- Security seals / cinchos associated to a transport on reception
-- =============================================================================

CREATE TABLE wms.warehouse_reception_seals (
    id              UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    reception_id    UUID        NOT NULL REFERENCES wms.warehouse_receptions(id),
    seal_number     VARCHAR(50) NOT NULL,
    registered_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_reception_seal UNIQUE (reception_id, seal_number)
);

-- =============================================================================
-- TABLE 4: wms.warehouse_transfers
-- Internal pallet relocations between warehouse locations
-- =============================================================================

CREATE TABLE wms.warehouse_transfers (
    id                          UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id             UUID         NOT NULL REFERENCES wms.organizations(id),
    branch_id                   UUID         NOT NULL REFERENCES wms.branches(id),

    -- Folio format: CAM-YYYY-NNNNNN
    folio                       VARCHAR(30)  NOT NULL UNIQUE,

    status                      VARCHAR(20)  NOT NULL DEFAULT 'COMPLETED'
                                    CONSTRAINT chk_wt_status CHECK (status IN ('DRAFT','CONFIRMED','COMPLETED','CANCELLED')),

    origin_location_id          UUID         NOT NULL REFERENCES wms.locations(id),
    destination_location_id     UUID         NOT NULL REFERENCES wms.locations(id),
    forklift_operator_id        UUID                  REFERENCES wms.forklift_operators(id),

    reason_code                 VARCHAR(30)  NOT NULL
                                    CONSTRAINT chk_wt_reason CHECK (reason_code IN (
                                        'OPT_ESPACIO','REUB_OPERATIVA','LIB_BAHIA',
                                        'CONSOLIDACION','SOL_CLIENTE','INCIDENCIA','OTRO'
                                    )),
    reason_label                VARCHAR(100),
    observations                TEXT,
    total_pallets               INTEGER      NOT NULL DEFAULT 0,
    total_pieces                NUMERIC(12,2) NOT NULL DEFAULT 0,
    distinct_skus               INTEGER      NOT NULL DEFAULT 0,

    cancelled_at                TIMESTAMPTZ,
    cancellation_reason         TEXT,
    cancelled_by                VARCHAR(100),

    version                     BIGINT       NOT NULL DEFAULT 1,
    created_at                  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by                  VARCHAR(36),
    updated_by                  VARCHAR(36)
);

-- =============================================================================
-- TABLE 5: wms.warehouse_transfer_items
-- Individual inventory items (UAs) moved in each transfer
-- =============================================================================

CREATE TABLE wms.warehouse_transfer_items (
    id              UUID    PRIMARY KEY DEFAULT uuid_generate_v4(),
    transfer_id     UUID    NOT NULL REFERENCES wms.warehouse_transfers(id),
    item_id         UUID    NOT NULL REFERENCES wms.inventory_items(id),
    pieces          NUMERIC(10,2) NOT NULL DEFAULT 0,
    pallet_code     VARCHAR(50),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- =============================================================================
-- TABLE 6: wms.warehouse_outbounds
-- Outbound dispatch headers (Salidas de Almacén F03)
-- =============================================================================

CREATE TABLE wms.warehouse_outbounds (
    id                      UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    organization_id         UUID         NOT NULL REFERENCES wms.organizations(id),
    branch_id               UUID         NOT NULL REFERENCES wms.branches(id),

    -- Folio format: SAL-YYYY-NNNNNN
    folio                   VARCHAR(30)  NOT NULL UNIQUE,

    status                  VARCHAR(20)  NOT NULL DEFAULT 'COMPLETED'
                                CONSTRAINT chk_wo_status CHECK (status IN ('DRAFT','CONFIRMED','COMPLETED','CANCELLED')),

    -- Client / Destination snapshot
    client_id               UUID         NOT NULL REFERENCES wms.clients(id),
    destination_id          UUID                  REFERENCES wms.client_destinations(id),
    destination_name        VARCHAR(200),
    destination_address     TEXT,

    -- Transport snapshot
    carrier_id              UUID                  REFERENCES wms.carriers(id),
    transport_type          VARCHAR(30)  NOT NULL DEFAULT 'TRAILER'
                                CONSTRAINT chk_wo_transport CHECK (transport_type IN ('CAMION','TORTON','TRAILER')),
    driver_name             VARCHAR(150) NOT NULL,
    economic_number         VARCHAR(30),
    tractor_plates          VARCHAR(20)  NOT NULL,
    box_plates              VARCHAR(20)  NOT NULL,
    seal_number             VARCHAR(50)  NOT NULL,
    remision_no             VARCHAR(60)  NOT NULL,

    -- Totals (denormalized for performance)
    total_pallets           INTEGER      NOT NULL DEFAULT 0,
    total_pieces            NUMERIC(12,2) NOT NULL DEFAULT 0,
    distinct_skus           INTEGER      NOT NULL DEFAULT 0,

    cancelled_at            TIMESTAMPTZ,
    cancellation_reason     TEXT,
    cancelled_by            VARCHAR(100),

    version                 BIGINT       NOT NULL DEFAULT 1,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by              VARCHAR(36),
    updated_by              VARCHAR(36)
);

-- =============================================================================
-- TABLE 7: wms.warehouse_outbound_items
-- Individual inventory items dispatched per outbound
-- =============================================================================

CREATE TABLE wms.warehouse_outbound_items (
    id              UUID    PRIMARY KEY DEFAULT uuid_generate_v4(),
    outbound_id     UUID    NOT NULL REFERENCES wms.warehouse_outbounds(id),
    item_id         UUID    NOT NULL REFERENCES wms.inventory_items(id),
    pieces          NUMERIC(10,2) NOT NULL DEFAULT 0,
    pallet_code     VARCHAR(50),
    lot_number      VARCHAR(50),
    expiration_date DATE,
    location_code   VARCHAR(50),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- =============================================================================
-- PERFORMANCE INDEXES
-- =============================================================================

-- warehouse_receptions
CREATE INDEX idx_wr_org_status      ON wms.warehouse_receptions (organization_id, status);
CREATE INDEX idx_wr_branch          ON wms.warehouse_receptions (branch_id);
CREATE INDEX idx_wr_client          ON wms.warehouse_receptions (client_id);
CREATE INDEX idx_wr_folio           ON wms.warehouse_receptions (folio);
CREATE INDEX idx_wr_doc_number      ON wms.warehouse_receptions (doc_number);
CREATE INDEX idx_wr_status          ON wms.warehouse_receptions (status);
CREATE INDEX idx_wr_created_at      ON wms.warehouse_receptions (created_at DESC);

-- warehouse_reception_pallets
CREATE INDEX idx_wrp_reception      ON wms.warehouse_reception_pallets (reception_id);
CREATE INDEX idx_wrp_pallet_code    ON wms.warehouse_reception_pallets (pallet_code);
CREATE INDEX idx_wrp_sku            ON wms.warehouse_reception_pallets (sku_id);
CREATE INDEX idx_wrp_inventory_item ON wms.warehouse_reception_pallets (inventory_item_id);

-- warehouse_reception_seals
CREATE INDEX idx_wrs_reception      ON wms.warehouse_reception_seals (reception_id);

-- warehouse_transfers
CREATE INDEX idx_wt_org_status      ON wms.warehouse_transfers (organization_id, status);
CREATE INDEX idx_wt_origin          ON wms.warehouse_transfers (origin_location_id);
CREATE INDEX idx_wt_destination     ON wms.warehouse_transfers (destination_location_id);
CREATE INDEX idx_wt_folio           ON wms.warehouse_transfers (folio);
CREATE INDEX idx_wt_created_at      ON wms.warehouse_transfers (created_at DESC);

-- warehouse_transfer_items
CREATE INDEX idx_wti_transfer       ON wms.warehouse_transfer_items (transfer_id);
CREATE INDEX idx_wti_item           ON wms.warehouse_transfer_items (item_id);

-- warehouse_outbounds
CREATE INDEX idx_wo_org_status      ON wms.warehouse_outbounds (organization_id, status);
CREATE INDEX idx_wo_client          ON wms.warehouse_outbounds (client_id);
CREATE INDEX idx_wo_folio           ON wms.warehouse_outbounds (folio);
CREATE INDEX idx_wo_created_at      ON wms.warehouse_outbounds (created_at DESC);

-- warehouse_outbound_items
CREATE INDEX idx_woi_outbound       ON wms.warehouse_outbound_items (outbound_id);
CREATE INDEX idx_woi_item           ON wms.warehouse_outbound_items (item_id);

-- =============================================================================
-- TRIGGERS: Auto-update updated_at timestamp
-- =============================================================================

CREATE OR REPLACE FUNCTION wms.fn_update_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_wr_updated_at
    BEFORE UPDATE ON wms.warehouse_receptions
    FOR EACH ROW EXECUTE FUNCTION wms.fn_update_timestamp();

CREATE TRIGGER trg_wrp_updated_at
    BEFORE UPDATE ON wms.warehouse_reception_pallets
    FOR EACH ROW EXECUTE FUNCTION wms.fn_update_timestamp();

CREATE TRIGGER trg_wt_updated_at
    BEFORE UPDATE ON wms.warehouse_transfers
    FOR EACH ROW EXECUTE FUNCTION wms.fn_update_timestamp();

CREATE TRIGGER trg_wo_updated_at
    BEFORE UPDATE ON wms.warehouse_outbounds
    FOR EACH ROW EXECUTE FUNCTION wms.fn_update_timestamp();
