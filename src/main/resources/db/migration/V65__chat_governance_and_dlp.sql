-- ============================================================================
-- V65: Chat Governance, Freeze/Archive Controls, DLP & Security Audit Events
-- ============================================================================

ALTER TABLE wms.chat_conversations
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN IF NOT EXISTS archived_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS archived_by UUID,
    ADD COLUMN IF NOT EXISTS retention_days INT;

ALTER TABLE wms.chat_messages
    ADD COLUMN IF NOT EXISTS original_content TEXT,
    ADD COLUMN IF NOT EXISTS deleted_by UUID;

ALTER TABLE wms.users
    ADD COLUMN IF NOT EXISTS chat_frozen_until TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS chat_frozen_reason VARCHAR(255);

CREATE TABLE IF NOT EXISTS wms.chat_security_events (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    branch_id UUID,
    conversation_id UUID,
    message_id UUID,
    user_id UUID,
    event_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    description TEXT,
    ip_address VARCHAR(64),
    user_agent TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_chat_sec_events_org_created
    ON wms.chat_security_events (organization_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_chat_conv_org_status
    ON wms.chat_conversations (organization_id, status);
