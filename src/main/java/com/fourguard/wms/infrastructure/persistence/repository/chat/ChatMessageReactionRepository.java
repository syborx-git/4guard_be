package com.fourguard.wms.infrastructure.persistence.repository.chat;

import com.fourguard.wms.infrastructure.persistence.entity.chat.ChatMessageReactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatMessageReactionRepository extends JpaRepository<ChatMessageReactionEntity, UUID> {

    List<ChatMessageReactionEntity> findByMessageIdIn(List<UUID> messageIds);

    Optional<ChatMessageReactionEntity> findByMessageIdAndUserIdAndEmoji(UUID messageId, UUID userId, String emoji);
}
