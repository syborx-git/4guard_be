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
    fk RECORD;
    tbl RECORD;
    col RECORD;
    sql_stmt TEXT;
BEGIN
    -- 2.1. Respaldar y eliminar temporalmente todas las constraints de Foreign Key del esquema 'wms'
    --      (Evita requerir privilegios de SUPERUSER necesarios para session_replication_role)
    DROP TABLE IF EXISTS temp_wms_foreign_keys;
    CREATE TEMP TABLE temp_wms_foreign_keys AS
    SELECT 
        c.conname AS constraint_name,
        format('%I.%I', n_rel.nspname, rel.relname) AS table_name,
        pg_get_constraintdef(c.oid) AS constraint_definition
    FROM pg_constraint c
    JOIN pg_namespace n ON n.oid = c.connamespace
    JOIN pg_class rel ON rel.oid = c.conrelid
    JOIN pg_namespace n_rel ON n_rel.oid = rel.relnamespace
    WHERE n.nspname = 'wms' AND c.contype = 'f';

    FOR fk IN SELECT table_name, constraint_name FROM temp_wms_foreign_keys LOOP
        EXECUTE format('ALTER TABLE %s DROP CONSTRAINT IF EXISTS %I', fk.table_name, fk.constraint_name);
    END LOOP;

    -- 2.2. Actualizar los UUIDs deterministas en todas las tablas y columnas UUID
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

    -- 2.3. Restaurar todas las constraints de Foreign Key
    FOR fk IN SELECT table_name, constraint_name, constraint_definition FROM temp_wms_foreign_keys LOOP
        EXECUTE format('ALTER TABLE %s ADD CONSTRAINT %I %s', fk.table_name, fk.constraint_name, fk.constraint_definition);
    END LOOP;

    DROP TABLE IF EXISTS temp_wms_foreign_keys;
END $$;
