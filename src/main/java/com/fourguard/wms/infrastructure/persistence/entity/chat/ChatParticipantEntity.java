package com.fourguard.wms.infrastructure.persistence.entity.chat;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_participants", uniqueConstraints = {
        @UniqueConstraint(name = "uq_chat_participant_conv_user", columnNames = {"conversation_id", "user_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatParticipantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "role", nullable = false, length = 30)
    @Builder.Default
    private String role = "MEMBER"; // OWNER, ADMIN, MEMBER

    @Column(name = "notification_pref", nullable = false, length = 20)
    @Builder.Default
    private String notificationPref = "ALL"; // ALL, MENTIONS, MUTED

    @Column(name = "last_read_at", nullable = false)
    private Instant lastReadAt;

    @Column(name = "last_read_message_id")
    private UUID lastReadMessageId;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (this.joinedAt == null) this.joinedAt = now;
        if (this.lastReadAt == null) this.lastReadAt = now;
        if (this.isActive == null) this.isActive = true;
    }
}
