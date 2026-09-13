-- =============================================================================
-- V12: Make Carrier Secondary Fields Flexible
-- Description: Permite que tax_id, contact_name, contact_phone, contact_email
--              sean opcionales (NULL) en wms.carriers, optimizando el alta rápida
--              de transportistas con sus campos esenciales (Razón Social,
--              Sobre Nombre, Estado Operativo, Tipo de Unidad, Observaciones).
-- =============================================================================

SET search_path TO wms, public;

ALTER TABLE wms.carriers
    ALTER COLUMN tax_id DROP NOT NULL,
    ALTER COLUMN contact_name DROP NOT NULL,
    ALTER COLUMN contact_phone DROP NOT NULL,
    ALTER COLUMN contact_email DROP NOT NULL;

-- Salidas de Almacén (F03 Outbound) - Agregar número económico de caja
ALTER TABLE wms.warehouse_outbounds
    ADD COLUMN IF NOT EXISTS box_economic_number VARCHAR(30);

