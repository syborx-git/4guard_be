ALTER TABLE chat_messages
    ADD COLUMN IF NOT EXISTS is_pinned BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS pinned_at TIMESTAMP WITH TIME ZONE NULL,
    ADD COLUMN IF NOT EXISTS pinned_by UUID NULL;

CREATE INDEX IF NOT EXISTS idx_chat_msg_conv_pinned
    ON chat_messages (conversation_id, is_pinned, pinned_at DESC)
    WHERE is_pinned = TRUE;
