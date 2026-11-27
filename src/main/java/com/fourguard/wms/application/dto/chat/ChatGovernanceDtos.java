package com.fourguard.wms.application.dto.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ChatGovernanceDtos {

    private ChatGovernanceDtos() {
    }

    public record PagedResponse<T>(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
    }

    public record ChatGovernanceOverviewDto(
            long totalChatUsers,
            long onlineUsers,
            long chatOnlyUsers,
            long chatOnlyOnline,
            long frozenUsers,
            long activeSpaces,
            long frozenSpaces,
            long archivedSpaces,
            long totalDms,
            long messagesLast7Days,
            long editedMessagesLast7Days,
            long deletedMessagesLast7Days,
            long totalAttachmentsCount,
            double totalAttachmentsSizeMb,
            long dlpAlertsLast7Days
    ) {
    }

    public record GovernanceSpaceDto(
            UUID id,
            String name,
            String description,
            String category,
            String status,
            UUID branchId,
            long memberCount,
            long messageCount,
            Instant lastMessageAt,
            Instant createdAt
    ) {
    }

    public record UpdateSpaceStatusRequest(
            @NotBlank(message = "El estado es obligatorio") String status
    ) {
    }

    public record GovernanceChatUserDto(
            UUID id,
            String username,
            String fullName,
            String email,
            String roleName,
            @JsonProperty("isChatOnly") boolean isChatOnly,
            @JsonProperty("isEnabled") boolean isEnabled,
            String status,
            boolean online,
            Instant lastLogin,
            Instant chatFrozenUntil,
            String chatFrozenReason,
            long spacesCount
    ) {
    }

    public record FreezeUserRequest(
            @NotBlank(message = "El motivo de congelamiento es obligatorio") String reason,
            Boolean chatOnlyFreeze,
            Integer durationHours
    ) {
    }

    public record GovernanceActionResponse(
            boolean success,
            String message
    ) {
    }

    public record GovernanceAuditMessageDto(
            UUID id,
            UUID conversationId,
            String conversationName,
            String conversationType,
            UUID senderId,
            String senderName,
            String content,
            String originalContent,
            @JsonProperty("isEdited") boolean isEdited,
            Instant editedAt,
            @JsonProperty("isDeleted") boolean isDeleted,
            Instant deletedAt,
            Instant createdAt
    ) {
    }

    public record GovernanceAttachmentDto(
            UUID id,
            UUID messageId,
            UUID conversationId,
            String conversationName,
            String senderName,
            String fileName,
            String contentType,
            long fileSize,
            Instant createdAt
    ) {
    }

    public record GovernanceAttachmentDownloadDto(
            UUID id,
            String fileName,
            String contentType,
            long fileSize,
            String storageUrl
    ) {
    }

    public record GovernanceDlpEventDto(
            UUID id,
            UUID conversationId,
            String conversationName,
            UUID userId,
            String userName,
            String eventType,
            String severity,
            String description,
            String ipAddress,
            Instant createdAt
    ) {
    }

    public record ReportDlpEventRequest(
            UUID conversationId,
            @NotBlank(message = "El tipo de evento DLP es obligatorio") String eventType,
            String description
    ) {
    }

    public record DlpReportResponse(
            boolean recorded
    ) {
    }

    public record BreakGlassRequest(
            @NotBlank(message = "El motivo es obligatorio")
            @Size(min = 10, message = "El motivo debe tener al menos 10 caracteres")
            String reason
    ) {
    }

    public record BreakGlassMessageDto(
            UUID id,
            String senderName,
            String content,
            String originalContent,
            @JsonProperty("isEdited") boolean isEdited,
            @JsonProperty("isDeleted") boolean isDeleted,
            Instant createdAt,
            long attachmentCount
    ) {
    }

    public record BreakGlassTranscriptDto(
            UUID conversationId,
            String conversationName,
            String conversationType,
            Instant auditedAt,
            String reason,
            List<BreakGlassMessageDto> messages
    ) {
    }
}
