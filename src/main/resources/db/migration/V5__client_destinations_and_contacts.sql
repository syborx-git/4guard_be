-- =============================================================================
-- MIGRATION V5: CLIENT EXTENSIONS — CORPORATE CONTACTS & PHYSICAL DESTINATIONS
-- =============================================================================
-- Agrega campos de contacto/dirección a wms.clients y crea las tablas
-- relacionales para la Matriz de Contactos Corporativos y las Direcciones
-- Físicas de Destino (Multi-Bodega / Multi-Planta / Ship-to Locations).
-- =============================================================================

-- 1. AMPLIAR TABLA MAESTRA DE CLIENTES
--    Agrega campos que el FE ya enviaba pero el BE descartaba silenciosamente.
-- --------------------------------------------------------------------------
ALTER TABLE wms.clients
    ADD COLUMN IF NOT EXISTS address             VARCHAR(300),
    ADD COLUMN IF NOT EXISTS phone               VARCHAR(50),
    ADD COLUMN IF NOT EXISTS email               VARCHAR(150),
    ADD COLUMN IF NOT EXISTS web_portal_password VARCHAR(255);

-- Índices de búsqueda: RFC / External ID por organización (RN-CLI-001)
CREATE UNIQUE INDEX IF NOT EXISTS uk_clients_org_external_id
    ON wms.clients (organization_id, external_id)
    WHERE external_id IS NOT NULL;

-- --------------------------------------------------------------------------
-- 2. TABLA DE MATRIZ DE CONTACTOS CORPORATIVOS (1:N con Clientes)
--    Contactos de Logística, Finanzas, Calidad, Compras, etc.
-- --------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS wms.client_contacts (
    id          UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    client_id   UUID        NOT NULL
                    REFERENCES wms.clients(id) ON DELETE CASCADE,
    name        VARCHAR(150) NOT NULL,
    department  VARCHAR(100) NOT NULL,
    phone       VARCHAR(50)  NOT NULL,
    email       VARCHAR(150) NOT NULL,
    is_primary  BOOLEAN      DEFAULT FALSE,
    created_at  TIMESTAMPTZ  DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  DEFAULT NOW(),
    created_by  VARCHAR(36),
    updated_by  VARCHAR(36)
);

-- Solo puede haber un contacto primario por cliente
CREATE UNIQUE INDEX IF NOT EXISTS uk_client_contacts_primary
    ON wms.client_contacts (client_id)
    WHERE is_primary = TRUE;

-- Índice de acceso rápido por cliente
CREATE INDEX IF NOT EXISTS idx_client_contacts_client_id
    ON wms.client_contacts (client_id);

-- --------------------------------------------------------------------------
-- 3. TABLA DE DIRECCIONES FÍSICAS DE DESTINO / BODEGAS / PLANTAS (1:N con Clientes)
--    Ship-to Locations: plantas de manufactura, CEDIS, almacenes de clientes.
-- --------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS wms.client_destinations (
    id                  UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    client_id           UUID        NOT NULL
                            REFERENCES wms.clients(id) ON DELETE CASCADE,
    destination_code    VARCHAR(50)  NOT NULL,
    plant_name          VARCHAR(200) NOT NULL,
    full_address        VARCHAR(500) NOT NULL,
    contact_person      VARCHAR(150) NOT NULL,
    phone               VARCHAR(50)  NOT NULL,
    status              VARCHAR(20)  DEFAULT 'ACTIVO',
    notes               TEXT,
    version             BIGINT       DEFAULT 1,
    created_at          TIMESTAMPTZ  DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  DEFAULT NOW(),
    created_by          VARCHAR(36),
    updated_by          VARCHAR(36),
    CONSTRAINT uk_client_destination_code UNIQUE (client_id, destination_code)
);

-- Índice principal por cliente (consultas de despacho / outbound)
CREATE INDEX IF NOT EXISTS idx_client_destinations_client_id
    ON wms.client_destinations (client_id);

-- Índice compuesto para filtrar destinos ACTIVOS por cliente (RN-CLI-007)
CREATE INDEX IF NOT EXISTS idx_client_destinations_active
    ON wms.client_destinations (client_id, status)
    WHERE status = 'ACTIVO';
