-- =============================================================================
-- V14: Migrate All Seed Entities to Production-Grade UUIDs (RFC 4122 v4)
-- Description: Detecta y actualiza dinámicamente todos los UUIDs con ceros artificiales
--              (0000xxxx-..., b800xxxx-..., etc.) en TODAS las tablas y columnas UUID
--              del esquema 'wms', garantizando sincronía total de Primary Keys
--              y Foreign Keys (carrier_id, client_id, forklift_operator_id, sku_id,
--              supplier_id, location_id, destination_id, etc.) sin romper ninguna relación.
-- =============================================================================

SET search_path TO wms, public;

-- 1. Función inmutable para generación determinista de UUID v4 (RFC 4122) a partir de cualquier seed
CREATE OR REPLACE FUNCTION wms.fn_deterministic_uuid_v4(seed_text TEXT) 
RETURNS UUID AS $$
DECLARE
    h VARCHAR(32);
    u_str VARCHAR(36);
BEGIN
    h := md5(lower(trim(seed_text)));
    -- Formato RFC 4122 v4: 8-4-4-4-12 con versión 4 y variante RFC 4122
    u_str := substr(h, 1, 8) || '-' ||
             substr(h, 9, 4) || '-' ||
             '4' || substr(h, 14, 3) || '-' ||
             to_hex(8 + (('x' || substr(h, 17, 1))::bit(4)::int % 4)) || substr(h, 18, 3) || '-' ||
             substr(h, 21, 12);
    RETURN u_str::UUID;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- 2. Bloque PL/pgSQL para migración atómica y universal en todas las tablas del esquema 'wms'
DO $$
DECLARE
    tbl RECORD;
    col RECORD;
    sql_stmt TEXT;
BEGIN
    -- Desactivar temporalmente constraints de FK y triggers durante la actualización atómica
    SET session_replication_role = 'replica';

    FOR tbl IN 
        SELECT table_name 
        FROM information_schema.tables 
        WHERE table_schema = 'wms' 
          AND table_type = 'BASE TABLE'
        ORDER BY table_name
    LOOP
        FOR col IN 
            SELECT column_name 
            FROM information_schema.columns 
            WHERE table_schema = 'wms' 
              AND table_name = tbl.table_name 
              AND data_type = 'uuid'
            ORDER BY ordinal_position
        LOOP
            sql_stmt := format(
                'UPDATE wms.%I SET %I = wms.fn_deterministic_uuid_v4(%I::text) WHERE %I::text LIKE ''0000%%'' OR %I::text LIKE ''%%-0000-0000-0000-%%'' OR %I::text LIKE ''b800%%''',
                tbl.table_name, col.column_name, col.column_name, col.column_name, col.column_name, col.column_name
            );
            EXECUTE sql_stmt;
        END LOOP;
    END LOOP;

    -- Restaurar modo de replicación normal
    SET session_replication_role = 'origin';
END $$;
