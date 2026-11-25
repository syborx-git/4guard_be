package com.fourguard.wms.application.usecase;

import com.fourguard.wms.application.dto.chat.ChatDtos.*;
import com.fourguard.wms.domain.enums.UserStatus;
import com.fourguard.wms.domain.ports.out.AuditLogRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.AuditLogEntity;
import com.fourguard.wms.infrastructure.persistence.entity.UserEntity;
import com.fourguard.wms.infrastructure.persistence.entity.chat.*;
import com.fourguard.wms.infrastructure.persistence.repository.UserJpaRepository;
import com.fourguard.wms.infrastructure.persistence.repository.chat.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ChatConversationRepository conversationRepo;
    private final ChatParticipantRepository participantRepo;
    private final ChatMessageRepository messageRepo;
    private final ChatMessageReactionRepository reactionRepo;
    private final ChatAttachmentRepository attachmentRepo;
    private final UserJpaRepository userRepo;
    private final AuditLogRepositoryPort auditLogRepositoryPort;
    private final SimpMessagingTemplate messagingTemplate;
    private final Optional<SimpUserRegistry> simpUserRegistry;

    public UserEntity getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado");
        }
        return userRepo.findByUsername(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
    }

    @Transactional(readOnly = true)
    public List<ChatUserDto> listOrganizationUsers(UUID branchId) {
        UserEntity me = getCurrentUser();
        UUID orgId = me.getOrganization().getId();
        Set<UUID> onlineIds = resolveOnlineUserIds();
        Set<String> wsUsernames = resolveOnlineWsUsernames();

        return userRepo.findByOrganizationId(orgId).stream()
                .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                .filter(u -> !u.getId().equals(me.getId()))
                .filter(u -> branchId == null || (u.getBranch() != null && branchId.equals(u.getBranch().getId())))
                .map(u -> {
                    boolean isOnline = onlineIds.contains(u.getId()) || wsUsernames.contains(u.getUsername());
                    return new ChatUserDto(
                            u.getId(),
                            u.getUsername(),
                            buildFullName(u),
                            u.getEmail(),
                            u.getRole() != null ? u.getRole().getName() : "USER",
                            u.getBranch() != null ? u.getBranch().getId() : null,
                            u.getStatus() == UserStatus.ACTIVE,
                            isOnline
                    );
                })
                .sorted(Comparator.comparing(ChatUserDto::online).reversed()
                        .thenComparing(ChatUserDto::fullName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ConversationSummaryDto> listMyConversations() {
        UserEntity me = getCurrentUser();
        UUID orgId = me.getOrganization().getId();
        List<ChatConversationEntity> conversations = conversationRepo.findActiveByOrganizationAndUser(orgId, me.getId());

        Map<UUID, UserEntity> userCache = new HashMap<>();
        userCache.put(me.getId(), me);

        return conversations.stream()
                .map(conv -> buildConversationSummary(conv, me, userCache))
                .toList();
    }

    @Transactional
    public ConversationSummaryDto getOrCreateDm(CreateDmRequest req) {
        UserEntity me = getCurrentUser();
        UUID orgId = me.getOrganization().getId();

        UserEntity target = userRepo.findById(req.targetUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario destino no encontrado"));

        if (!target.getOrganization().getId().equals(orgId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes iniciar chat con usuarios de otra organización");
        }
        if (target.getId().equals(me.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No puedes iniciar un DM contigo mismo");
        }

        ChatConversationEntity conv = conversationRepo.findDirectMessageBetween(orgId, me.getId(), target.getId())
                .orElseGet(() -> {
                    ChatConversationEntity created = conversationRepo.save(ChatConversationEntity.builder()
                            .organizationId(orgId)
                            .branchId(me.getBranch() != null ? me.getBranch().getId() : null)
                            .type("DM")
                            .createdBy(me.getId())
                            .build());

                    participantRepo.save(ChatParticipantEntity.builder()
                            .conversationId(created.getId())
                            .userId(me.getId())
                            .role("MEMBER")
                            .build());

                    participantRepo.save(ChatParticipantEntity.builder()
                            .conversationId(created.getId())
                            .userId(target.getId())
                            .role("MEMBER")
                            .build());

                    return created;
                });

        Map<UUID, UserEntity> cache = Map.of(me.getId(), me, target.getId(), target);
        return buildConversationSummary(conv, me, new HashMap<>(cache));
    }

    private void requireManagerOrDirector(UserEntity me, String errorMessage) {
        String roleName = me.getRole() != null ? me.getRole().getName().toUpperCase() : "";
        boolean isManagerOrDirector = roleName.contains("CEO")
                || roleName.contains("DIRECTOR")
                || roleName.contains("GERENTE")
                || roleName.contains("OPERATIONS_MANAGER")
                || roleName.contains("ADMIN");
        if (!isManagerOrDirector) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, errorMessage);
        }
    }

    @Transactional
    public ConversationSummaryDto createSpace(CreateSpaceRequest req) {
        UserEntity me = getCurrentUser();
        requireManagerOrDirector(me, "Solo Gerentes o Directivos pueden crear Espacios");

        UUID orgId = me.getOrganization().getId();
        String category = req.category() != null && !req.category().isBlank() ? req.category().trim() : null;
        ChatConversationEntity space = conversationRepo.save(ChatConversationEntity.builder()
                .organizationId(orgId)
                .branchId(req.branchId())
                .type("SPACE")
                .name(req.name().trim())
                .description(req.description())
                .icon(req.icon() != null && !req.icon().isBlank() ? req.icon() : "tag")
                .category(category)
                .createdBy(me.getId())
                .build());

        Set<UUID> memberIds = new LinkedHashSet<>();
        memberIds.add(me.getId());
        if (req.memberIds() != null) {
            memberIds.addAll(req.memberIds());
        }

        for (UUID uid : memberIds) {
            userRepo.findById(uid).ifPresent(u -> {
                if (u.getOrganization().getId().equals(orgId)) {
                    participantRepo.save(ChatParticipantEntity.builder()
                            .conversationId(space.getId())
                            .userId(uid)
                            .role(uid.equals(me.getId()) ? "OWNER" : "MEMBER")
                            .build());
                }
            });
        }

        return buildConversationSummary(space, me, new HashMap<>(Map.of(me.getId(), me)));
    }

    @Transactional
    public ConversationSummaryDto updateSpaceMembers(UUID conversationId, UpdateSpaceMembersRequest req) {
        UserEntity me = getCurrentUser();
        requireManagerOrDirector(me, "Solo Gerentes o Directivos pueden administrar miembros de Espacios");

        UUID orgId = me.getOrganization().getId();
        ChatConversationEntity space = conversationRepo.findById(conversationId)
                .filter(c -> orgId.equals(c.getOrganizationId()) && "SPACE".equals(c.getType()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Espacio no encontrado"));

        Set<UUID> desiredIds = new LinkedHashSet<>(req.memberIds());
        desiredIds.add(space.getCreatedBy());
        desiredIds.add(me.getId());

        List<ChatParticipantEntity> existing = participantRepo.findByConversationIdAndIsActiveTrue(conversationId);
        for (ChatParticipantEntity p : existing) {
            if (!desiredIds.contains(p.getUserId()) && !"OWNER".equals(p.getRole())) {
                p.setIsActive(false);
                participantRepo.save(p);
            }
        }

        for (UUID uid : desiredIds) {
            userRepo.findById(uid).ifPresent(u -> {
                if (u.getOrganization().getId().equals(orgId)) {
                    ChatParticipantEntity part = participantRepo.findByConversationIdAndUserId(conversationId, uid)
                            .orElseGet(() -> ChatParticipantEntity.builder()
                                    .conversationId(conversationId)
                                    .userId(uid)
                                    .role(uid.equals(space.getCreatedBy()) ? "OWNER" : "MEMBER")
                                    .build());
                    part.setIsActive(true);
                    participantRepo.save(part);
                }
            });
        }

        return buildConversationSummary(space, me, new HashMap<>(Map.of(me.getId(), me)));
    }

    @Transactional(readOnly = true)
    public List<MessageDto> getMessages(UUID conversationId, Instant before, int limit) {
        UserEntity me = getCurrentUser();
        requireParticipant(conversationId, me.getId());

        int safeLimit = Math.min(Math.max(limit, 1), 100);
        List<ChatMessageEntity> entities = new ArrayList<>(
                before == null
                        ? messageRepo.findByConversationIdAndParentMessageIdIsNullOrderByCreatedAtDesc(
                                conversationId, PageRequest.of(0, safeLimit))
                        : messageRepo.findByConversationIdAndParentMessageIdIsNullAndCreatedAtBeforeOrderByCreatedAtDesc(
                                conversationId, before, PageRequest.of(0, safeLimit))
        );

        Collections.reverse(entities);
        return enrichMessages(entities, me.getId());
    }

    @Transactional
    public MessageDto sendMessage(UUID conversationId, SendMessageRequest req) {
        UserEntity me = getCurrentUser();
        ChatConversationEntity conv = requireOrgConversationAndParticipant(conversationId, me);

        ChatMessageEntity saved = messageRepo.save(ChatMessageEntity.builder()
                .conversationId(conversationId)
                .senderId(me.getId())
                .parentMessageId(req.parentMessageId())
                .content(req.content().trim())
                .messageType(req.messageType() != null && !req.messageType().isBlank() ? req.messageType() : "TEXT")
                .metadataJson(req.metadataJson())
                .build());

        if (req.attachments() != null && !req.attachments().isEmpty()) {
            for (AttachmentInput att : req.attachments()) {
                validateAttachment(att);
                attachmentRepo.save(ChatAttachmentEntity.builder()
                        .messageId(saved.getId())
                        .fileName(att.fileName().trim())
                        .contentType(att.contentType().trim().toLowerCase())
                        .fileSize(att.fileSize())
                        .storageUrl(att.storageUrl().trim())
                        .build());
            }
        }

        conv.setLastMessageAt(saved.getCreatedAt());
        conversationRepo.save(conv);

        // Actualizar lastReadAt del remitente automáticamente
        participantRepo.findByConversationIdAndUserId(conversationId, me.getId()).ifPresent(p -> {
            p.setLastReadAt(saved.getCreatedAt());
            p.setLastReadMessageId(saved.getId());
            participantRepo.save(p);
        });

        MessageDto dto = enrichMessages(List.of(saved), me.getId()).get(0);
        broadcastEvent("MESSAGE_CREATED", conversationId, me, dto);
        return dto;
    }

    @Transactional
    public MessageDto editMessage(UUID messageId, EditMessageRequest req) {
        UserEntity me = getCurrentUser();
        ChatMessageEntity msg = messageRepo.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mensaje no encontrado"));
        requireOrgConversationAndParticipant(msg.getConversationId(), me);

        if (!msg.getSenderId().equals(me.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo puedes editar tus propios mensajes");
        }
        if (Boolean.TRUE.equals(msg.getIsDeleted())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No puedes editar un mensaje eliminado");
        }

        msg.setContent(req.content().trim());
        msg.setIsEdited(true);
        msg.setEditedAt(Instant.now());
        ChatMessageEntity saved = messageRepo.save(msg);

        MessageDto dto = enrichMessages(List.of(saved), me.getId()).get(0);
        broadcastEvent("MESSAGE_UPDATED", msg.getConversationId(), me, dto);
        return dto;
    }

    @Transactional
    public MessageDto deleteMessage(UUID messageId) {
        UserEntity me = getCurrentUser();
        ChatMessageEntity msg = messageRepo.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mensaje no encontrado"));

        ChatConversationEntity conv = conversationRepo.findById(msg.getConversationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversación no encontrada"));
        if (!me.getOrganization().getId().equals(conv.getOrganizationId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso denegado a esta organización");
        }

        String roleName = me.getRole() != null ? me.getRole().getName().toUpperCase() : "";
        boolean isOrgAdmin = roleName.contains("ADMIN") || roleName.contains("CEO") || roleName.contains("DIRECTOR");
        boolean isActiveParticipant = participantRepo.existsByConversationIdAndUserIdAndIsActiveTrue(msg.getConversationId(), me.getId());
        if (!(msg.getSenderId().equals(me.getId()) && isActiveParticipant) && !isOrgAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para eliminar este mensaje");
        }

        msg.setIsDeleted(true);
        msg.setDeletedAt(Instant.now());
        msg.setContent("Mensaje eliminado");
        ChatMessageEntity saved = messageRepo.save(msg);

        MessageDto dto = enrichMessages(List.of(saved), me.getId()).get(0);
        broadcastEvent("MESSAGE_DELETED", msg.getConversationId(), me, dto);
        return dto;
    }

    @Transactional
    public MessageDto toggleReaction(UUID messageId, ToggleReactionRequest req) {
        UserEntity me = getCurrentUser();
        ChatMessageEntity msg = messageRepo.findById(messageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mensaje no encontrado"));
        requireParticipant(msg.getConversationId(), me.getId());

        Optional<ChatMessageReactionEntity> existing = reactionRepo.findByMessageIdAndUserIdAndEmoji(
                messageId, me.getId(), req.emoji().trim());

        if (existing.isPresent()) {
            reactionRepo.delete(existing.get());
        } else {
            reactionRepo.save(ChatMessageReactionEntity.builder()
                    .messageId(messageId)
                    .userId(me.getId())
                    .emoji(req.emoji().trim())
                    .build());
        }

        MessageDto dto = enrichMessages(List.of(msg), me.getId()).get(0);
        broadcastEvent("REACTION_UPDATED", msg.getConversationId(), me, dto);
        return dto;
    }

    @Transactional
    public void markAsRead(UUID conversationId) {
        UserEntity me = getCurrentUser();
        requireOrgConversationAndParticipant(conversationId, me);
        ChatParticipantEntity part = participantRepo.findByConversationIdAndUserId(conversationId, me.getId())
                .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "No eres participante activo de esta conversación"));

        part.setLastReadAt(Instant.now());
        messageRepo.findTopByConversationIdOrderByCreatedAtDesc(conversationId)
                .ifPresent(last -> part.setLastReadMessageId(last.getId()));
        participantRepo.save(part);
    }

    @Transactional
    public void updateNotificationPref(UUID conversationId, UpdateNotificationPrefRequest req) {
        UserEntity me = getCurrentUser();
        requireOrgConversationAndParticipant(conversationId, me);
        ChatParticipantEntity part = participantRepo.findByConversationIdAndUserId(conversationId, me.getId())
                .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "No eres participante activo de esta conversación"));

        String pref = req.notificationPref().trim().toUpperCase();
        if (!Set.of("ALL", "MENTIONS", "MUTED").contains(pref)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Preferencia inválida (ALL, MENTIONS, MUTED)");
        }
        part.setNotificationPref(pref);
        participantRepo.save(part);
    }

    public void broadcastTyping(UUID conversationId) {
        UserEntity me = getCurrentUser();
        requireParticipant(conversationId, me.getId());
        broadcastEvent("TYPING", conversationId, me, Map.of("userId", me.getId(), "username", me.getUsername(), "fullName", buildFullName(me)));
    }

    private void requireParticipant(UUID conversationId, UUID userId) {
        if (!participantRepo.existsByConversationIdAndUserIdAndIsActiveTrue(conversationId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a esta conversación");
        }
    }

    private ChatConversationEntity requireOrgConversationAndParticipant(UUID conversationId, UserEntity me) {
        ChatConversationEntity conv = conversationRepo.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversación no encontrada"));
        if (!me.getOrganization().getId().equals(conv.getOrganizationId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso denegado a esta organización");
        }
        requireParticipant(conversationId, me.getId());
        return conv;
    }

    private void validateAttachment(AttachmentInput att) {
        if (att.fileSize() <= 0 || att.fileSize() > 2L * 1024 * 1024) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El tamaño del archivo debe ser mayor a 0 y máximo 2 MB");
        }
        String mime = att.contentType() != null ? att.contentType().trim().toLowerCase() : "";
        Set<String> allowedMimes = Set.of("image/jpeg", "image/png", "image/webp", "image/gif", "application/pdf");
        if (!allowedMimes.contains(mime)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de archivo no permitido: " + mime);
        }
        String url = att.storageUrl() != null ? att.storageUrl().trim() : "";
        if (url.length() > 3_000_000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo excede el límite permitido");
        }
        boolean safePrefix = url.startsWith("data:image/jpeg;base64,")
                || url.startsWith("data:image/png;base64,")
                || url.startsWith("data:image/webp;base64,")
                || url.startsWith("data:image/gif;base64,")
                || url.startsWith("data:application/pdf;base64,")
                || url.startsWith("https://")
                || url.startsWith("/api/");
        if (!safePrefix) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "URL o formato de adjunto no permitido");
        }
    }

    private ConversationSummaryDto buildConversationSummary(
            ChatConversationEntity conv,
            UserEntity me,
            Map<UUID, UserEntity> userCache
    ) {
        Set<UUID> onlineIds = resolveOnlineUserIds();
        Set<String> wsUsernames = resolveOnlineWsUsernames();

        List<ChatParticipantEntity> parts = participantRepo.findByConversationIdAndIsActiveTrue(conv.getId());
        List<ParticipantDto> partDtos = parts.stream().map(p -> {
            UserEntity u = userCache.computeIfAbsent(p.getUserId(), id -> userRepo.findById(id).orElse(null));
            boolean isOnline = onlineIds.contains(p.getUserId()) || (u != null && wsUsernames.contains(u.getUsername()));
            return new ParticipantDto(
                    p.getUserId(),
                    u != null ? u.getUsername() : "usuario",
                    u != null ? buildFullName(u) : "Usuario",
                    p.getRole(),
                    p.getNotificationPref(),
                    p.getLastReadAt(),
                    isOnline
            );
        }).toList();

        ChatParticipantEntity myPart = parts.stream()
                .filter(p -> p.getUserId().equals(me.getId()))
                .findFirst()
                .orElse(null);

        Instant lastRead = myPart != null ? myPart.getLastReadAt() : Instant.EPOCH;
        long unread = messageRepo.countByConversationIdAndCreatedAtAfterAndSenderIdNot(conv.getId(), lastRead, me.getId());

        MessageDto lastMsg = messageRepo.findTopByConversationIdOrderByCreatedAtDesc(conv.getId())
                .map(m -> enrichMessages(List.of(m), me.getId()).get(0))
                .orElse(null);

        String displayName = conv.getName();
        if ("DM".equals(conv.getType()) && (displayName == null || displayName.isBlank())) {
            displayName = partDtos.stream()
                    .filter(p -> !p.userId().equals(me.getId()))
                    .map(ParticipantDto::fullName)
                    .findFirst()
                    .orElse("Mensaje Directo");
        }

        return new ConversationSummaryDto(
                conv.getId(),
                conv.getOrganizationId(),
                conv.getBranchId(),
                conv.getType(),
                displayName,
                conv.getDescription(),
                conv.getIcon(),
                conv.getCategory(),
                conv.getEntityType(),
                conv.getEntityId(),
                conv.getLastMessageAt(),
                unread,
                lastMsg,
                partDtos,
                myPart != null ? myPart.getNotificationPref() : "ALL"
        );
    }

    private List<MessageDto> enrichMessages(List<ChatMessageEntity> entities, UUID currentUserId) {
        if (entities.isEmpty()) return List.of();

        List<UUID> msgIds = entities.stream().map(ChatMessageEntity::getId).toList();
        List<ChatMessageReactionEntity> allReactions = reactionRepo.findByMessageIdIn(msgIds);
        List<ChatAttachmentEntity> allAttachments = attachmentRepo.findByMessageIdIn(msgIds);

        Set<UUID> userIds = new HashSet<>();
        entities.forEach(m -> userIds.add(m.getSenderId()));
        allReactions.forEach(r -> userIds.add(r.getUserId()));

        Map<UUID, UserEntity> usersMap = userRepo.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, u -> u));

        Map<UUID, List<ChatMessageReactionEntity>> reactionsByMsg = allReactions.stream()
                .collect(Collectors.groupingBy(ChatMessageReactionEntity::getMessageId));
        Map<UUID, List<ChatAttachmentEntity>> attachmentsByMsg = allAttachments.stream()
                .collect(Collectors.groupingBy(ChatAttachmentEntity::getMessageId));

        return entities.stream().map(m -> {
            UserEntity sender = usersMap.get(m.getSenderId());
            List<ReactionSummaryDto> reactionDtos = reactionsByMsg.getOrDefault(m.getId(), List.of()).stream()
                    .collect(Collectors.groupingBy(ChatMessageReactionEntity::getEmoji))
                    .entrySet().stream()
                    .map(entry -> new ReactionSummaryDto(
                            entry.getKey(),
                            entry.getValue().size(),
                            entry.getValue().stream().anyMatch(r -> r.getUserId().equals(currentUserId)),
                            entry.getValue().stream()
                                    .map(r -> usersMap.get(r.getUserId()))
                                    .filter(Objects::nonNull)
                                    .map(UserEntity::getUsername)
                                    .toList()
                    ))
                    .toList();

            List<AttachmentDto> attachmentDtos = attachmentsByMsg.getOrDefault(m.getId(), List.of()).stream()
                    .map(a -> new AttachmentDto(a.getId(), a.getFileName(), a.getContentType(), a.getFileSize(), a.getStorageUrl()))
                    .toList();

            return new MessageDto(
                    m.getId(),
                    m.getConversationId(),
                    m.getSenderId(),
                    sender != null ? sender.getUsername() : "usuario",
                    sender != null ? buildFullName(sender) : "Usuario",
                    m.getParentMessageId(),
                    Boolean.TRUE.equals(m.getIsDeleted()) ? "Mensaje eliminado" : m.getContent(),
                    m.getMessageType(),
                    m.getMetadataJson(),
                    Boolean.TRUE.equals(m.getIsEdited()),
                    m.getEditedAt(),
                    Boolean.TRUE.equals(m.getIsDeleted()),
                    m.getCreatedAt(),
                    reactionDtos,
                    attachmentDtos,
                    0L
            );
        }).toList();
    }

    private void broadcastEvent(String eventType, UUID conversationId, UserEntity sender, Object payload) {
        try {
            ChatWsEventDto event = new ChatWsEventDto(
                    eventType,
                    conversationId,
                    sender.getId(),
                    sender.getUsername(),
                    payload,
                    Instant.now()
            );
            messagingTemplate.convertAndSend("/topic/chat." + conversationId, event);

            // Notificar también a la cola personal de cada participante para badges en vivo
            List<ChatParticipantEntity> parts = participantRepo.findByConversationIdAndIsActiveTrue(conversationId);
            for (ChatParticipantEntity p : parts) {
                if (!p.getUserId().equals(sender.getId())) {
                    userRepo.findById(p.getUserId()).ifPresent(u ->
                            messagingTemplate.convertAndSendToUser(u.getUsername(), "/queue/chat", event)
                    );
                }
            }
        } catch (Exception ex) {
            log.warn("[CHAT-WS] Error difundiendo evento '{}' en conversación {}: {}", eventType, conversationId, ex.getMessage());
        }
    }

    private Set<UUID> resolveOnlineUserIds() {
        try {
            OffsetDateTime since = OffsetDateTime.now(ZoneOffset.UTC).minusHours(24);
            List<AuditLogEntity> loginEvents = auditLogRepositoryPort.findByActionAndCreatedAtAfter("LOGIN", since);
            if (loginEvents.isEmpty()) return Set.of();

            Map<UUID, AuditLogEntity> latestLogins = loginEvents.stream()
                    .filter(e -> e.getUserId() != null && e.getCreatedAt() != null)
                    .collect(Collectors.toMap(
                            AuditLogEntity::getUserId,
                            e -> e,
                            (a, b) -> a.getCreatedAt().isAfter(b.getCreatedAt()) ? a : b
                    ));

            return latestLogins.values().stream()
                    .filter(login -> auditLogRepositoryPort
                            .findLastLogoutForUserAfter(login.getUserId(), login.getCreatedAt())
                            .isEmpty())
                    .map(AuditLogEntity::getUserId)
                    .collect(Collectors.toSet());
        } catch (Exception ex) {
            log.warn("[CHAT-PRESENCE] Error resolviendo usuarios en línea: {}", ex.getMessage());
            return Set.of();
        }
    }

    private Set<String> resolveOnlineWsUsernames() {
        return simpUserRegistry
                .map(reg -> reg.getUsers().stream().map(u -> u.getName()).collect(Collectors.toSet()))
                .orElse(Set.of());
    }

    private String buildFullName(UserEntity u) {
        String first = u.getFirstName() != null ? u.getFirstName().trim() : "";
        String last = u.getLastName() != null ? u.getLastName().trim() : "";
        String full = (first + " " + last).trim();
        return full.isEmpty() ? u.getUsername() : full;
    }
}
