-- =============================================================================
-- V53: Create user_webauthn_credentials
-- Tablas y llaves públicas COSE FIDO2/WebAuthn para autenticación biométrica
-- (Face ID / Touch ID / TEE) en Terminales RF y Caseta de Seguridad (Zero Biometric Knowledge).
-- =============================================================================

CREATE TABLE IF NOT EXISTS wms.user_webauthn_credentials (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id              UUID NOT NULL REFERENCES wms.users(id) ON DELETE CASCADE,
    credential_id        VARCHAR(512) NOT NULL UNIQUE,
    public_key_cose      TEXT NOT NULL,
    sign_count           BIGINT NOT NULL DEFAULT 0,
    aaguid               VARCHAR(64) NULL,
    device_name          VARCHAR(120) NOT NULL DEFAULT 'Terminal RF Field Device',
    attestation_format   VARCHAR(32) NOT NULL DEFAULT 'none',
    is_active            BOOLEAN NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_used_at         TIMESTAMPTZ NULL,
    version              BIGINT NOT NULL DEFAULT 1
);

CREATE INDEX IF NOT EXISTS idx_webauthn_cred_id ON wms.user_webauthn_credentials (credential_id);
CREATE INDEX IF NOT EXISTS idx_webauthn_user_id ON wms.user_webauthn_credentials (user_id);

CREATE TABLE IF NOT EXISTS wms.webauthn_challenges (
    challenge_token      VARCHAR(128) PRIMARY KEY,
    user_id              UUID NULL REFERENCES wms.users(id) ON DELETE CASCADE,
    operation_type       VARCHAR(32) NOT NULL,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at           TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_webauthn_challenges_exp ON wms.webauthn_challenges (expires_at);

COMMENT ON TABLE wms.user_webauthn_credentials IS
    'Llaves públicas COSE y contadores FIDO2/WebAuthn para autenticación biométrica de montacarguistas y guardias.';
COMMENT ON TABLE wms.webauthn_challenges IS
    'Desafíos criptográficos transitorios (nonces de un solo uso) para prevenir ataques de repetición (Replay Attacks).';

