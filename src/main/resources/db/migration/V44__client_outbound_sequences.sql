-- =============================================================================
-- V44: Client-Specific Outbound Folio Sequences and Client Short Code
-- Description: Agrega la tabla de secuencias atómicas independientes por cliente
--              para salidas (wms.client_outbound_sequences) y asegura la columna
--              short_code en wms.clients para la nomenclatura de folios.
-- =============================================================================

SET search_path TO wms, public;

-- 1. Agregar columna short_code en wms.clients si no existe
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'wms' AND table_name = 'clients' AND column_name = 'short_code'
    ) THEN
        ALTER TABLE wms.clients ADD COLUMN short_code VARCHAR(10);
    END IF;
END $$;

-- 2. Poblar short_code inicial según nombres y RFCs conocidos
UPDATE wms.clients
SET short_code = 'MNE'
WHERE upper(name) LIKE '%MARCAS NESTLE%' OR upper(tax_id) LIKE 'MNE%';

UPDATE wms.clients
SET short_code = 'NME'
WHERE upper(name) LIKE '%NESTLE MEXICO%' OR upper(tax_id) LIKE 'NME%';

UPDATE wms.clients
SET short_code = 'QUA'
WHERE upper(name) LIKE '%QUALAMEX%' OR upper(tax_id) LIKE 'QUA%';

-- Fallback para cualquier otro cliente sin short_code: primeros 3 caracteres alfanuméricos de tax_id o name
UPDATE wms.clients
SET short_code = UPPER(SUBSTRING(REGEXP_REPLACE(COALESCE(tax_id, name, 'CLI'), '[^a-zA-Z0-9]', '', 'g') FROM 1 FOR 3))
WHERE short_code IS NULL OR short_code = '';

-- 3. Crear tabla de control de secuencias por cliente para salidas
CREATE TABLE IF NOT EXISTS wms.client_outbound_sequences (
    client_id       UUID PRIMARY KEY REFERENCES wms.clients(id) ON DELETE CASCADE,
    current_seq     BIGINT NOT NULL DEFAULT 0,
    updated_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 4. Sembrar secuencias iniciales a partir de salidas ya existentes en base de datos
INSERT INTO wms.client_outbound_sequences (client_id, current_seq, updated_at)
SELECT 
    client_id,
    COUNT(*) AS current_seq,
    NOW()
FROM wms.warehouse_outbounds
WHERE client_id IS NOT NULL
GROUP BY client_id
ON CONFLICT (client_id) DO UPDATE
SET current_seq = EXCLUDED.current_seq, updated_at = NOW();

-- 5. Función atómica para obtener el siguiente valor de secuencia de salida para un cliente
CREATE OR REPLACE FUNCTION wms.fn_next_client_outbound_seq(p_client_id UUID)
RETURNS BIGINT AS $$
DECLARE
    v_next BIGINT;
BEGIN
    INSERT INTO wms.client_outbound_sequences (client_id, current_seq, updated_at)
    VALUES (p_client_id, 1, NOW())
    ON CONFLICT (client_id)
    DO UPDATE SET 
        current_seq = wms.client_outbound_sequences.current_seq + 1,
        updated_at = NOW()
    RETURNING current_seq INTO v_next;

    RETURN v_next;
END;
$$ LANGUAGE plpgsql;
