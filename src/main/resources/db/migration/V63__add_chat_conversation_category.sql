-- ============================================================================
-- V63: Clasificación libre de Espacios de Chat (departamento / área / tipo)
-- ============================================================================

ALTER TABLE chat_conversations
    ADD COLUMN IF NOT EXISTS category VARCHAR(80) NULL;

CREATE INDEX IF NOT EXISTS idx_chat_conv_org_category
    ON chat_conversations (organization_id, category);
