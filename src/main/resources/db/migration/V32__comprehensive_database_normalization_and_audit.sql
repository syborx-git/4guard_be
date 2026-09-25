-- =================================================================================================
-- FLYWAY MIGRATION V32: Comprehensive Database Normalization, Integrity Constraints & Indexing
-- Author: 4GUARD Database Architecture & Quality Engineering Team
-- =================================================================================================

SET search_path TO wms, public;

-- -------------------------------------------------------------------------------------------------
-- 1. NORMALIZATION: PHONE NUMBERS (10 DIGITS IFT STANDARD ACROSS ALL DOMAINS)
-- -------------------------------------------------------------------------------------------------

-- Clean existing non-digits if any and apply 10-digit standard validation
UPDATE wms.clients 
SET phone = REGEXP_REPLACE(phone, '[^0-9]', '', 'g')
WHERE phone IS NOT NULL AND phone <> '';

UPDATE wms.clients 
SET phone = SUBSTRING(phone FROM 1 FOR 10)
WHERE phone IS NOT NULL AND LENGTH(phone) > 10;

ALTER TABLE wms.clients
    DROP CONSTRAINT IF EXISTS chk_clients_phone_10_digits;

ALTER TABLE wms.clients
    ADD CONSTRAINT chk_clients_phone_10_digits
    CHECK (phone ~ '^[0-9]{10}$' OR phone IS NULL);

-- Client Contacts
UPDATE wms.client_contacts 
SET phone = REGEXP_REPLACE(phone, '[^0-9]', '', 'g')
WHERE phone IS NOT NULL AND phone <> '';

UPDATE wms.client_contacts 
SET phone = SUBSTRING(phone FROM 1 FOR 10)
WHERE phone IS NOT NULL AND LENGTH(phone) > 10;

ALTER TABLE wms.client_contacts
    DROP CONSTRAINT IF EXISTS chk_client_contacts_phone_10_digits;

ALTER TABLE wms.client_contacts
    ADD CONSTRAINT chk_client_contacts_phone_10_digits
    CHECK (phone ~ '^[0-9]{10}$' OR phone IS NULL);

-- Carriers Contact Phone
UPDATE wms.carriers 
SET contact_phone = REGEXP_REPLACE(contact_phone, '[^0-9]', '', 'g')
WHERE contact_phone IS NOT NULL AND contact_phone <> '';

UPDATE wms.carriers 
SET contact_phone = SUBSTRING(contact_phone FROM 1 FOR 10)
WHERE contact_phone IS NOT NULL AND LENGTH(contact_phone) > 10;

ALTER TABLE wms.carriers
    DROP CONSTRAINT IF EXISTS chk_carriers_contact_phone_10_digits;

ALTER TABLE wms.carriers
    ADD CONSTRAINT chk_carriers_contact_phone_10_digits
    CHECK (contact_phone ~ '^[0-9]{10}$' OR contact_phone IS NULL);

-- Supplier Contacts Phone
UPDATE wms.supplier_contacts 
SET phone = REGEXP_REPLACE(phone, '[^0-9]', '', 'g')
WHERE phone IS NOT NULL AND phone <> '';

UPDATE wms.supplier_contacts 
SET phone = SUBSTRING(phone FROM 1 FOR 10)
WHERE phone IS NOT NULL AND LENGTH(phone) > 10;

ALTER TABLE wms.supplier_contacts
    DROP CONSTRAINT IF EXISTS chk_supplier_contacts_phone_10_digits;

ALTER TABLE wms.supplier_contacts
    ADD CONSTRAINT chk_supplier_contacts_phone_10_digits
    CHECK (phone ~ '^[0-9]{10}$' OR phone IS NULL);

-- -------------------------------------------------------------------------------------------------
-- 2. NORMALIZATION: SECURITY PRE-CHECKINS (CASETA DE SEGURIDAD & FORMATO F01)
-- -------------------------------------------------------------------------------------------------

-- Column sizing optimizations
ALTER TABLE wms.security_pre_checkins
    ALTER COLUMN tractor_plates TYPE VARCHAR(15),
    ALTER COLUMN box_plates TYPE VARCHAR(15),
    ALTER COLUMN driver_license TYPE VARCHAR(30),
    ALTER COLUMN economic_number TYPE VARCHAR(25),
    ALTER COLUMN no_eco_tractor TYPE VARCHAR(25),
    ALTER COLUMN box_economic_number TYPE VARCHAR(25),
    ALTER COLUMN box_dimensions TYPE VARCHAR(30);

-- Status & Operation Constraints
ALTER TABLE wms.security_pre_checkins
    DROP CONSTRAINT IF EXISTS chk_security_precheckins_status;

ALTER TABLE wms.security_pre_checkins
    ADD CONSTRAINT chk_security_precheckins_status
    CHECK (status IN ('PENDING_DRIVER', 'SUBMITTED', 'COMPLETED', 'CANCELLED'));

ALTER TABLE wms.security_pre_checkins
    DROP CONSTRAINT IF EXISTS chk_security_precheckins_op_type;

ALTER TABLE wms.security_pre_checkins
    ADD CONSTRAINT chk_security_precheckins_op_type
    CHECK (operation_type IN ('CARGA', 'DESCARGA'));

-- GIN Index for JSONB Checklist Querying (Fast Analytical Queries on Inspection Criteria)
CREATE INDEX IF NOT EXISTS idx_security_precheckins_checklist_gin
    ON wms.security_pre_checkins USING GIN (checklist_data);

-- -------------------------------------------------------------------------------------------------
-- 3. 3NF RELATIONSHIP INTEGRATION: LINK RECEPTIONS & OUTBOUNDS TO SECURITY PRE-CHECKINS
-- -------------------------------------------------------------------------------------------------

ALTER TABLE wms.warehouse_receptions
    ADD COLUMN IF NOT EXISTS pre_checkin_id UUID REFERENCES wms.security_pre_checkins(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_warehouse_receptions_pre_checkin
    ON wms.warehouse_receptions(pre_checkin_id);

ALTER TABLE wms.warehouse_outbounds
    ADD COLUMN IF NOT EXISTS pre_checkin_id UUID REFERENCES wms.security_pre_checkins(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_warehouse_outbounds_pre_checkin
    ON wms.warehouse_outbounds(pre_checkin_id);

-- -------------------------------------------------------------------------------------------------
-- 4. 1NF NORMALIZED CHILD TABLE FOR DETAILED CHECKLIST AUDIT TRAIL
-- -------------------------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS wms.security_checklist_items (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    pre_checkin_id      UUID NOT NULL REFERENCES wms.security_pre_checkins(id) ON DELETE CASCADE,
    category            VARCHAR(30) NOT NULL, -- EPP, DOCUMENTS, BOX_INSPECTION, SEALS, SIGNATURE
    criterion_code      VARCHAR(50) NOT NULL, -- EPP_BOOTS, EPP_HAIRNET, BOX_CLEAN, BOX_PESTS, etc.
    criterion_label     VARCHAR(150) NOT NULL,
    result_value        VARCHAR(10) NOT NULL, -- SI, NO, NA
    observations        TEXT,
    is_compliant        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT uk_precheckin_criterion UNIQUE (pre_checkin_id, criterion_code)
);

CREATE INDEX IF NOT EXISTS idx_checklist_items_precheckin ON wms.security_checklist_items(pre_checkin_id);
CREATE INDEX IF NOT EXISTS idx_checklist_items_compliance ON wms.security_checklist_items(criterion_code, is_compliant);

COMMENT ON TABLE wms.security_checklist_items IS 'Normalized 1NF storage of individual inspection criteria for Format F01-PO-CP-7.1.3-03 analytical reporting';
