package com.fourguard.wms.application.dto.chat;

import java.time.Instant;
import java.util.UUID;

/**
 * Lightweight JPQL projection for chat attachment metadata.
 * Deliberately excludes storage_url (Base64) to keep JVM memory usage minimal.
 */
public record AttachmentMetadataRow(
        UUID id,
        UUID messageId,
        UUID conversationId,
        String conversationName,
        String conversationType,
        UUID senderId,
        String fileName,
        String contentType,
        Long fileSize,
        Instant createdAt
) {
}
