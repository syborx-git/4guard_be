package com.fourguard.wms.application.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ChatDtos {

        private ChatDtos() {
        }

        public record ChatUserDto(
                        UUID id,
                        String username,
                        String fullName,
                        String email,
                        String roleName,
                        UUID branchId,
                        boolean active,
                        boolean online) {
        }

        public record ParticipantDto(
                        UUID userId,
                        String username,
                        String fullName,
                        String role,
                        String notificationPref,
                        Instant lastReadAt,
                        boolean online) {
        }

        public record ReactionSummaryDto(
                        String emoji,
                        long count,
                        boolean reactedByMe,
                        List<String> usernames) {
        }

        public record AttachmentDto(
                        UUID id,
                        String fileName,
                        String contentType,
                        long fileSize,
                        String storageUrl) {
        }

        public record MessageDto(
                        UUID id,
                        UUID conversationId,
                        UUID senderId,
                        String senderUsername,
                        String senderFullName,
                        UUID parentMessageId,
                        String content,
                        String messageType,
                        String metadataJson,
                        boolean isEdited,
                        Instant editedAt,
                        boolean isDeleted,
                        Instant createdAt,
                        List<ReactionSummaryDto> reactions,
                        List<AttachmentDto> attachments,
                        long replyCount) {
        }

        public record ConversationSummaryDto(
                        UUID id,
                        UUID organizationId,
                        UUID branchId,
                        String type,
                        String name,
                        String description,
                        String icon,
                        String category,
                        String entityType,
                        String entityId,
                        Instant lastMessageAt,
                        long unreadCount,
                        MessageDto lastMessage,
                        List<ParticipantDto> participants,
                        String myNotificationPref) {
        }

        public record CreateDmRequest(
                        @NotNull UUID targetUserId) {
        }

        public record CreateSpaceRequest(
                        @NotBlank String name,
                        String description,
                        String icon,
                        String category,
                        UUID branchId,
                        List<UUID> memberIds) {
        }

        public record UpdateSpaceMembersRequest(
                        @NotNull List<UUID> memberIds) {
        }

        public record AttachmentInput(
                        @NotBlank String fileName,
                        @NotBlank String contentType,
                        long fileSize,
                        @NotBlank String storageUrl) {
        }

        public record SendMessageRequest(
                        @NotBlank String content,
                        UUID parentMessageId,
                        String messageType,
                        String metadataJson,
                        List<AttachmentInput> attachments) {
        }

        public record EditMessageRequest(
                        @NotBlank String content) {
        }

        public record ToggleReactionRequest(
                        @NotBlank String emoji) {
        }

        public record UpdateNotificationPrefRequest(
                        @NotBlank String notificationPref) {
        }

        public record ChatWsEventDto(
                        String eventType, // MESSAGE_CREATED, MESSAGE_UPDATED, MESSAGE_DELETED, REACTION_UPDATED, TYPING
                        UUID conversationId,
                        UUID senderId,
                        String senderUsername,
                        Object payload,
                        Instant timestamp) {
        }
}
