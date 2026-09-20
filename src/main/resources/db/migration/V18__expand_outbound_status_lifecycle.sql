-- =============================================================================
-- V18: Expand Outbound Status Lifecycle & Add Ramp / Authorization Fields
-- Description: Desacopla el ciclo de vida de Salidas de Almacén (Outbound F03)
--              en 5 fases operativas:
--              1. REGISTERED  - Caseta / Vigilancia (Pre-registro de transporte)
--              2. ASSIGNED    - Mesa Administrativa (Asignación de rampa y montacargas)
--              3. IN_PROGRESS - Terminal Montacargas en Andén (Surtido y escaneo UAs)
--              4. LOADED      - Carga Concluida (Registro de sellos/marchamos)
--              5. COMPLETED   - Cierre Administrativo y Despacho Formal (Descuento inventario)
--              6. CANCELLED   - Cancelación Extraordinaria con liberación de rampa
-- =============================================================================

SET search_path TO wms, public;

-- 1. Actualizar constraint de estatus en wms.warehouse_outbounds
ALTER TABLE wms.warehouse_outbounds
    DROP CONSTRAINT IF EXISTS chk_wo_status;

ALTER TABLE wms.warehouse_outbounds
    ADD CONSTRAINT chk_wo_status
    CHECK (status IN ('DRAFT', 'REGISTERED', 'ASSIGNED', 'IN_PROGRESS', 'LOADED', 'COMPLETED', 'CANCELLED'));

-- 2. Agregar columnas de Rampa, Observaciones y Cierre/Autorización
ALTER TABLE wms.warehouse_outbounds
    ADD COLUMN IF NOT EXISTS ramp_id UUID REFERENCES wms.locations(id),
    ADD COLUMN IF NOT EXISTS observations TEXT,
    ADD COLUMN IF NOT EXISTS completed_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS leader_authorized_by VARCHAR(100);

-- 3. Permitir que seal_number sea flexible al momento del pre-registro en caseta
ALTER TABLE wms.warehouse_outbounds
    ALTER COLUMN seal_number DROP NOT NULL;

-- 4. Índice de performance para rampa
CREATE INDEX IF NOT EXISTS idx_wo_ramp_id
    ON wms.warehouse_outbounds (ramp_id);
