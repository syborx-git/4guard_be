-- ============================================================================
-- V58: Horario de comida y ventana de pausa anticipada en turnos operativos
-- ============================================================================

ALTER TABLE wms.wms_shifts
    ADD COLUMN IF NOT EXISTS meal_start_time TIME NULL,
    ADD COLUMN IF NOT EXISTS meal_end_time TIME NULL,
    ADD COLUMN IF NOT EXISTS meal_early_pause_minutes INT NOT NULL DEFAULT 10,
    ADD COLUMN IF NOT EXISTS has_meal_break BOOLEAN NOT NULL DEFAULT TRUE;

-- Población inicial para los turnos semilla existentes
UPDATE wms.wms_shifts
SET meal_start_time = '10:00:00',
    meal_end_time = '10:30:00',
    meal_early_pause_minutes = 10,
    has_meal_break = TRUE
WHERE code = 'TRN-MAT-01' AND meal_start_time IS NULL;

UPDATE wms.wms_shifts
SET meal_start_time = '18:00:00',
    meal_end_time = '18:45:00',
    meal_early_pause_minutes = 10,
    has_meal_break = TRUE
WHERE code = 'TRN-VES-01' AND meal_start_time IS NULL;

UPDATE wms.wms_shifts
SET meal_start_time = '02:00:00',
    meal_end_time = '03:00:00',
    meal_early_pause_minutes = 10,
    has_meal_break = TRUE
WHERE code = 'TRN-NOC-01' AND meal_start_time IS NULL;

UPDATE wms.wms_shifts
SET meal_start_time = '14:00:00',
    meal_end_time = '15:00:00',
    meal_early_pause_minutes = 10,
    has_meal_break = TRUE
WHERE code = 'TRN-MIX-01' AND meal_start_time IS NULL;

