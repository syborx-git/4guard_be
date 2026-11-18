-- ==============================================================================
-- 4GUARD WMS - FLYWAY MIGRATION V60
-- Descripcion: Resolucion del conflicto operativo de turnos en Toluca (TRN-MAT-01 vs TRN-MIX-01)
--              y optimizacion de indice unico para soportar soft-deletes.
-- ==============================================================================

SET search_path TO wms, public;

-- 1. Resolver conflicto de dia Sabado:
-- TRN-MAT-01 es Turno Matutino de Lunes a Viernes. TRN-MIX-01 cubre Sabado y Domingo.
DELETE FROM wms.shift_operating_days
WHERE shift_id IN (SELECT id FROM wms.wms_shifts WHERE code = 'TRN-MAT-01')
  AND day_of_week = 'SATURDAY';

-- 2. Asegurar que TRN-MAT-01 mantenga Lunes a Viernes
INSERT INTO wms.shift_operating_days (shift_id, day_of_week)
SELECT s.id, d.day
FROM wms.wms_shifts s
CROSS JOIN (VALUES ('MONDAY'), ('TUESDAY'), ('WEDNESDAY'), ('THURSDAY'), ('FRIDAY')) AS d(day)
WHERE s.code = 'TRN-MAT-01'
ON CONFLICT DO NOTHING;

-- 3. Reemplazar constraint estatico por indice unico condicional a no-eliminados si aplica
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'uk_wms_shifts_code_branch'
    ) THEN
        ALTER TABLE wms.wms_shifts DROP CONSTRAINT uk_wms_shifts_code_branch;
        CREATE UNIQUE INDEX IF NOT EXISTS uk_wms_shifts_code_branch_active 
            ON wms.wms_shifts(code, branch_id) 
            WHERE is_deleted = FALSE;
    END IF;
END $$;
