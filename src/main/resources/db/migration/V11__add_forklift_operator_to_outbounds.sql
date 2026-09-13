-- =============================================================================
-- V11: Add Forklift Operator to Outbounds Schema
-- Description: Agrega la columna forklift_operator_id a wms.warehouse_outbounds
--              para permitir la asignación y trazabilidad de montacarguistas
--              en los despachos de salida de almacén (F03).
-- =============================================================================

SET search_path TO wms, public;

ALTER TABLE wms.warehouse_outbounds
    ADD COLUMN IF NOT EXISTS forklift_operator_id UUID REFERENCES wms.forklift_operators(id);

CREATE INDEX IF NOT EXISTS idx_wo_forklift_operator
    ON wms.warehouse_outbounds (forklift_operator_id);
