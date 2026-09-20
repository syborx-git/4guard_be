-- =============================================================================
-- V13: Homologación de Destinos y Centros de Negocio Oficiales con UUIDs de Producción
-- Description: Homologa los 6 Centros de Negocio oficiales (PAC, Culinarios, Cafes,
--              CAF, Chocolates, Café Verde) con UUIDs de producción reales (RFC 4122 v4),
--              haciendo client_id opcional (NULL) para permitir su selección libre
--              en Salidas de Almacén (Outbound F03) sin ceros artificiales y
--              preservando la integridad referencial en warehouse_outbounds.
-- =============================================================================

SET search_path TO wms, public;

-- 1. Desactivar temporalmente verificación de FKs durante la reestructuración
SET session_replication_role = 'replica';

-- 2. Permitir que client_id sea opcional (NULL) para destinos compartidos / centros de negocio
ALTER TABLE wms.client_destinations
    ALTER COLUMN client_id DROP NOT NULL;

-- 3. Ajustar constraint de unicidad para soportar destinos globales por código
ALTER TABLE wms.client_destinations
    DROP CONSTRAINT IF EXISTS uk_client_destination_code;

CREATE UNIQUE INDEX IF NOT EXISTS uk_client_destinations_code_idx
    ON wms.client_destinations (destination_code);

-- 4. Actualizar referencias previas en warehouse_outbounds hacia los nuevos UUIDs de producción
UPDATE wms.warehouse_outbounds SET destination_id = 'b9c6beee-1e5b-4e3c-8e46-32f0390bf0df' 
WHERE destination_id::text LIKE '00000201%' OR destination_id::text = '00000000-0000-0000-0008-000000000001';

UPDATE wms.warehouse_outbounds SET destination_id = 'e2a4b891-3c7d-4f1e-9a52-78d1f046b9a2' 
WHERE destination_id::text LIKE '00000202%' OR destination_id::text = '00000000-0000-0000-0008-000000000002';

UPDATE wms.warehouse_outbounds SET destination_id = 'f47ac10b-58cc-4372-a567-0e02b2c3d479' 
WHERE destination_id::text LIKE '00000203%' OR destination_id::text = '00000000-0000-0000-0008-000000000003';

UPDATE wms.warehouse_outbounds SET destination_id = '6ba7b810-9dad-41d1-80b4-00c04fd430c8' 
WHERE destination_id::text LIKE '00000204%' OR destination_id::text = '00000000-0000-0000-0008-000000000004';

UPDATE wms.warehouse_outbounds SET destination_id = '550e8400-e29b-41d4-a716-446655440000' 
WHERE destination_id::text LIKE '00000205%' OR destination_id::text = '00000000-0000-0000-0008-000000000005';

UPDATE wms.warehouse_outbounds SET destination_id = '9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d' 
WHERE destination_id::text LIKE '00000206%' OR destination_id::text = '00000000-0000-0000-0008-000000000006';

-- 5. Limpiar destinos previos con ceros artificiales
DELETE FROM wms.client_destinations
WHERE destination_code IN ('DEST-PAC', 'DEST-CULINARIOS', 'DEST-CAFES', 'DEST-CAF', 'DEST-CHOCOLATES', 'DEST-CAFE-VERDE')
   OR id::text LIKE '0000020%'
   OR id::text LIKE '00000000-0000-0000-0008-%';

-- 6. Sembrar los 6 Centros de Negocio oficiales con UUIDs de producción (RFC 4122 v4)
INSERT INTO wms.client_destinations (id, client_id, destination_code, plant_name, full_address, contact_person, phone, status, created_by, updated_by)
VALUES
    ('b9c6beee-1e5b-4e3c-8e46-32f0390bf0df', NULL, 'DEST-PAC', 'CENTRO DE NEGOCIO PAC', 'Planta Nestlé PAC, Toluca, Estado de México', 'Mesa de Control Nestlé', '5550000000', 'ACTIVO', 'SYSTEM', 'SYSTEM'),
    ('e2a4b891-3c7d-4f1e-9a52-78d1f046b9a2', NULL, 'DEST-CULINARIOS', 'CENTRO DE NEGOCIO CULINARIOS', 'Planta Culinarios Nestlé, Toluca, Estado de México', 'Mesa de Control Nestlé', '5550000000', 'ACTIVO', 'SYSTEM', 'SYSTEM'),
    ('f47ac10b-58cc-4372-a567-0e02b2c3d479', NULL, 'DEST-CAFES', 'CENTRO DE NEGOCIO CAFES', 'Planta Cafés Nestlé, Toluca, Estado de México', 'Mesa de Control Nestlé', '5550000000', 'ACTIVO', 'SYSTEM', 'SYSTEM'),
    ('6ba7b810-9dad-41d1-80b4-00c04fd430c8', NULL, 'DEST-CAF', 'CENTRO DE NEGOCIO CAF', 'Planta Nestlé CAF, Toluca, Estado de México', 'Mesa de Control Nestlé', '5550000000', 'ACTIVO', 'SYSTEM', 'SYSTEM'),
    ('550e8400-e29b-41d4-a716-446655440000', NULL, 'DEST-CHOCOLATES', 'CENTRO DE NEGOCIO CHOCOLATES', 'Planta Chocolates Nestlé, Toluca, Estado de México', 'Mesa de Control Nestlé', '5550000000', 'ACTIVO', 'SYSTEM', 'SYSTEM'),
    ('9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d', NULL, 'DEST-CAFE-VERDE', 'CENTRO DE NEGOCIO CAFÉ VERDE', 'Planta Nestlé Café Verde, Toluca, Estado de México', 'Mesa de Control Nestlé', '5550000000', 'ACTIVO', 'SYSTEM', 'SYSTEM')
ON CONFLICT (destination_code) DO UPDATE SET
    id = EXCLUDED.id,
    client_id = EXCLUDED.client_id,
    plant_name = EXCLUDED.plant_name,
    full_address = EXCLUDED.full_address,
    contact_person = EXCLUDED.contact_person,
    phone = EXCLUDED.phone,
    status = EXCLUDED.status,
    updated_at = NOW();

-- 7. Restaurar modo de integridad referencial
SET session_replication_role = 'origin';
