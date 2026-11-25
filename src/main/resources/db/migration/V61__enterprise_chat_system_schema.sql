-- ============================================================================
-- V61: Sistema de Mensajería Empresarial (Estilo Google Chat) & Modo Chat-Only
-- ADR-022 | Multi-tenant (organization_id) | UUID + UTC (ADR-008)
-- ============================================================================

CREATE TABLE IF NOT EXISTS chat_conversations (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    branch_id UUID NULL,
    type VARCHAR(30) NOT NULL, -- DM, SPACE, THREAD
    name VARCHAR(150) NULL,
    description VARCHAR(500) NULL,
    icon VARCHAR(60) NULL,
    entity_type VARCHAR(50) NULL, -- FOLIO, REMISION, INCIDENCE, SSCC
    entity_id VARCHAR(100) NULL,
    created_by UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    last_message_at TIMESTAMP WITH TIME ZONE NULL
);

CREATE INDEX IF NOT EXISTS idx_chat_conv_org_last_msg
    ON chat_conversations (organization_id, last_message_at DESC);

CREATE INDEX IF NOT EXISTS idx_chat_conv_entity
    ON chat_conversations (organization_id, entity_type, entity_id);

CREATE TABLE IF NOT EXISTS chat_participants (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES chat_conversations(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    role VARCHAR(30) NOT NULL DEFAULT 'MEMBER', -- OWNER, ADMIN, MEMBER
    notification_pref VARCHAR(20) NOT NULL DEFAULT 'ALL', -- ALL, MENTIONS, MUTED
    last_read_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    last_read_message_id UUID NULL,
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_chat_participant_conv_user UNIQUE (conversation_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_chat_part_user_active
    ON chat_participants (user_id, is_active);

CREATE TABLE IF NOT EXISTS chat_messages (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES chat_conversations(id) ON DELETE CASCADE,
    sender_id UUID NOT NULL,
    parent_message_id UUID NULL REFERENCES chat_messages(id) ON DELETE SET NULL,
    content TEXT NOT NULL,
    message_type VARCHAR(30) NOT NULL DEFAULT 'TEXT', -- TEXT, SYSTEM, ENTITY_REF
    metadata_json TEXT NULL,
    is_edited BOOLEAN NOT NULL DEFAULT FALSE,
    edited_at TIMESTAMP WITH TIME ZONE NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_chat_msg_conv_created
    ON chat_messages (conversation_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_chat_msg_parent
    ON chat_messages (parent_message_id, created_at ASC);

CREATE TABLE IF NOT EXISTS chat_message_reactions (
    id UUID PRIMARY KEY,
    message_id UUID NOT NULL REFERENCES chat_messages(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    emoji VARCHAR(24) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_chat_reaction_msg_user_emoji UNIQUE (message_id, user_id, emoji)
);

CREATE INDEX IF NOT EXISTS idx_chat_react_msg
    ON chat_message_reactions (message_id);

CREATE TABLE IF NOT EXISTS chat_attachments (
    id UUID PRIMARY KEY,
    message_id UUID NOT NULL REFERENCES chat_messages(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(120) NOT NULL,
    file_size BIGINT NOT NULL DEFAULT 0,
    storage_url TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_chat_att_msg
    ON chat_attachments (message_id);
