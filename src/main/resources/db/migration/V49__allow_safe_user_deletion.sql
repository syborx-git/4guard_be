-- ==============================================================================
-- 4GUARD WMS — Flyway Migration V49
-- Archivo: V49__allow_safe_user_deletion.sql
-- Descripción: Flexibilización de Foreign Keys hacia wms.users para permitir la
--              eliminación segura de cuentas sin destruir la trazabilidad histórica
--              ni colisionar con la política inmutable WORM de wms.audit_logs.
-- ==============================================================================

SET search_path TO wms, public;

-- 1. Bitácora de Auditoría Relacional (WORM):
--    Se elimina la constraint FK restrictiva sobre user_id en la tabla audit_logs
--    porque audit_logs es inmutable (WORM) y no admite UPDATE ni DELETE.
--    El user_id histórico se conserva permanentemente sin bloquear la eliminación
--    física de usuarios de prueba o dados de baja en wms.users.
ALTER TABLE wms.audit_logs ALTER COLUMN user_id DROP NOT NULL;
ALTER TABLE wms.audit_logs DROP CONSTRAINT IF EXISTS audit_logs_user_id_fkey;

-- 2. Incidencias de Almacén: reportado por pasa a SET NULL al eliminar el usuario
ALTER TABLE wms.incidences DROP CONSTRAINT IF EXISTS incidences_reported_by_id_fkey;
ALTER TABLE wms.incidences ADD CONSTRAINT incidences_reported_by_id_fkey 
    FOREIGN KEY (reported_by_id) REFERENCES wms.users(id) ON DELETE SET NULL;

-- 3. Metadatos de Inspección de Transporte (Caseta): registered_by pasa a SET NULL
ALTER TABLE wms.carrier_vehicle_metadata DROP CONSTRAINT IF EXISTS carrier_vehicle_metadata_registered_by_fkey;
ALTER TABLE wms.carrier_vehicle_metadata ADD CONSTRAINT carrier_vehicle_metadata_registered_by_fkey 
    FOREIGN KEY (registered_by) REFERENCES wms.users(id) ON DELETE SET NULL;

-- 4. Desviaciones de Calidad QM: detected_by_id pasa a SET NULL
ALTER TABLE wms.quality_deviations ALTER COLUMN detected_by_id DROP NOT NULL;
ALTER TABLE wms.quality_deviations DROP CONSTRAINT IF EXISTS quality_deviations_detected_by_id_fkey;
ALTER TABLE wms.quality_deviations ADD CONSTRAINT quality_deviations_detected_by_id_fkey 
    FOREIGN KEY (detected_by_id) REFERENCES wms.users(id) ON DELETE SET NULL;

-- 5. Dictámenes de Liberación de Calidad QM: released_by_user_id pasa a SET NULL
ALTER TABLE wms.quality_releases ALTER COLUMN released_by_user_id DROP NOT NULL;
ALTER TABLE wms.quality_releases DROP CONSTRAINT IF EXISTS quality_releases_released_by_user_id_fkey;
ALTER TABLE wms.quality_releases ADD CONSTRAINT quality_releases_released_by_user_id_fkey 
    FOREIGN KEY (released_by_user_id) REFERENCES wms.users(id) ON DELETE SET NULL;
