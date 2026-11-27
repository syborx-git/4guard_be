package com.fourguard.wms.application.usecase;

import com.fourguard.wms.application.dto.chat.AttachmentMetadataRow;
import com.fourguard.wms.application.dto.chat.ChatDtos;
import com.fourguard.wms.application.dto.chat.ChatGovernanceDtos.*;
import com.fourguard.wms.domain.enums.UserStatus;
import com.fourguard.wms.domain.ports.out.AuditLogRepositoryPort;
import com.fourguard.wms.domain.ports.out.TokenBlacklistPort;
import com.fourguard.wms.infrastructure.persistence.entity.UserEntity;
import com.fourguard.wms.infrastructure.persistence.entity.chat.ChatAttachmentEntity;
import com.fourguard.wms.infrastructure.persistence.entity.chat.ChatConversationEntity;
import com.fourguard.wms.infrastructure.persistence.entity.chat.ChatMessageEntity;
import com.fourguard.wms.infrastructure.persistence.entity.chat.ChatParticipantEntity;
import com.fourguard.wms.infrastructure.persistence.entity.chat.ChatSecurityEventEntity;
import com.fourguard.wms.infrastructure.persistence.repository.UserJpaRepository;
import com.fourguard.wms.infrastructure.persistence.repository.chat.ChatAttachmentRepository;
import com.fourguard.wms.infrastructure.persistence.repository.chat.ChatConversationRepository;
import com.fourguard.wms.infrastructure.persistence.repository.chat.ChatMessageRepository;
import com.fourguard.wms.infrastructure.persistence.repository.chat.ChatParticipantRepository;
import com.fourguard.wms.infrastructure.persistence.repository.chat.ChatSecurityEventJpaRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatGovernanceService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final Set<String> DIRECTOR_ROLES = Set.of("CEO", "DIRECTOR", "ADMIN", "SUPER_ADMIN");
    private static final Set<String> VALID_SPACE_STATUSES = Set.of("ACTIVE", "FROZEN", "ARCHIVED");
    private static final Set<String> VALID_USER_FILTERS = Set.of("ALL", "CHAT_ONLY", "WMS", "FROZEN");
    private static final Set<String> VALID_AUDIT_MSG_TYPES = Set.of("ALL", "EDITED", "DELETED");

    private final ChatConversationRepository conversationRepo;
    private final ChatParticipantRepository participantRepo;
    private final ChatMessageRepository messageRepo;
    private final ChatAttachmentRepository attachmentRepo;
    private final ChatSecurityEventJpaRepository securityEventRepo;
    private final UserJpaRepository userRepo;
    private final AuditLogRepositoryPort auditLogRepositoryPort;
    private final TokenBlacklistPort tokenBlacklistPort;
    private final SimpMessagingTemplate messagingTemplate;
    private final Optional<SimpUserRegistry> simpUserRegistry;

    private UserEntity getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado");
        }
        return userRepo.findByUsername(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
    }

    private UserEntity requireDirector() {
        UserEntity me = getCurrentUser();
        String roleName = me.getRole() != null && me.getRole().getName() != null
                ? me.getRole().getName().trim().toUpperCase()
                : "";
        if (!DIRECTOR_ROLES.contains(roleName)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Acceso restringido exclusivamente a Dirección (CEO, DIRECTOR, ADMIN, SUPER_ADMIN)"
            );
        }
        return me;
    }

    private Pageable buildPageable(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return PageRequest.of(safePage, safeSize);
    }

    private int clampDays(int days) {
        return Math.min(Math.max(days, 1), 365);
    }

    private String toSearchPattern(String search) {
        if (search == null || search.isBlank()) {
            return "";
        }
        return "%" + search.trim().toLowerCase() + "%";
    }

    @Transactional(readOnly = true)
    public ChatGovernanceOverviewDto getOverview() {
        UserEntity me = requireDirector();
        UUID orgId = me.getOrganization().getId();
        Instant now = Instant.now();
        Instant sevenDaysAgo = now.minus(7, ChronoUnit.DAYS);

        List<UserEntity> orgUsers = userRepo.findByOrganizationId(orgId);
        Set<UUID> onlineIds = ChatSupportHelper.resolveOnlineUserIds(auditLogRepositoryPort);
        Set<String> wsUsernames = ChatSupportHelper.resolveOnlineWsUsernames(simpUserRegistry);

        long totalChatUsers = 0;
        long onlineUsers = 0;
        long chatOnlyUsers = 0;
        long chatOnlyOnline = 0;
        long frozenUsers = 0;

        for (UserEntity u : orgUsers) {
            boolean frozen = ChatSupportHelper.isUserGovernanceFrozen(u, now);
            boolean activeAccount = u.getStatus() == UserStatus.ACTIVE && Boolean.TRUE.equals(u.getIsEnabled());
            boolean countsAsChatUser = u.getStatus() == UserStatus.ACTIVE || frozen;
            boolean isChatOnly = u.getRole() != null && "CHAT_ONLY".equalsIgnoreCase(u.getRole().getName());
            boolean online = activeAccount && (onlineIds.contains(u.getId()) || wsUsernames.contains(u.getUsername()));

            if (countsAsChatUser) {
                totalChatUsers++;
                if (isChatOnly) {
                    chatOnlyUsers++;
                }
            }
            if (online) {
                onlineUsers++;
                if (isChatOnly) {
                    chatOnlyOnline++;
                }
            }
            if (frozen) {
                frozenUsers++;
            }
        }

        long activeSpaces = conversationRepo.countByOrganizationIdAndTypeAndStatus(orgId, "SPACE", "ACTIVE");
        long frozenSpaces = conversationRepo.countByOrganizationIdAndTypeAndStatus(orgId, "SPACE", "FROZEN");
        long archivedSpaces = conversationRepo.countByOrganizationIdAndTypeAndStatus(orgId, "SPACE", "ARCHIVED");
        long totalDms = conversationRepo.countByOrganizationIdAndType(orgId, "DM");

        long messagesLast7Days = messageRepo.countByOrganizationAndCreatedAtAfter(orgId, sevenDaysAgo);
        long editedMessagesLast7Days = messageRepo.countEditedByOrganizationAndCreatedAtAfter(orgId, sevenDaysAgo);
        long deletedMessagesLast7Days = messageRepo.countDeletedByOrganizationAndCreatedAtAfter(orgId, sevenDaysAgo);

        long totalAttachmentsCount = attachmentRepo.countByOrganizationId(orgId);
        long totalBytes = attachmentRepo.sumFileSizeByOrganizationId(orgId);
        double totalAttachmentsSizeMb = Math.round((totalBytes / (1024.0 * 1024.0)) * 100.0) / 100.0;
        long dlpAlertsLast7Days = securityEventRepo.countByOrganizationIdAndCreatedAtAfter(orgId, sevenDaysAgo);

        return new ChatGovernanceOverviewDto(
                totalChatUsers,
                onlineUsers,
                chatOnlyUsers,
                chatOnlyOnline,
                frozenUsers,
                activeSpaces,
                frozenSpaces,
                archivedSpaces,
                totalDms,
                messagesLast7Days,
                editedMessagesLast7Days,
                deletedMessagesLast7Days,
                totalAttachmentsCount,
                totalAttachmentsSizeMb,
                dlpAlertsLast7Days
        );
    }

    @Transactional(readOnly = true)
    public PagedResponse<GovernanceSpaceDto> listSpaces(String status, String search, int page, int size) {
        UserEntity me = requireDirector();
        UUID orgId = me.getOrganization().getId();

        String normalizedStatus = "";
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status.trim())) {
            normalizedStatus = status.trim().toUpperCase();
            if (!VALID_SPACE_STATUSES.contains(normalizedStatus)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado de espacio inválido (ACTIVE, FROZEN, ARCHIVED)");
            }
        }

        Pageable pageable = buildPageable(page, size);
        Page<ChatConversationEntity> spacesPage = conversationRepo.findGovernanceSpaces(
                orgId, normalizedStatus, toSearchPattern(search), pageable
        );

        List<UUID> spaceIds = spacesPage.getContent().stream().map(ChatConversationEntity::getId).toList();
        Map<UUID, Long> memberCounts = spaceIds.isEmpty()
                ? Map.of()
                : toCountMap(participantRepo.countActiveByConversationIds(spaceIds));
        Map<UUID, Long> messageCounts = spaceIds.isEmpty()
                ? Map.of()
                : toCountMap(messageRepo.countByConversationIds(spaceIds));

        List<GovernanceSpaceDto> content = spacesPage.getContent().stream()
                .map(space -> toSpaceDto(
                        space,
                        memberCounts.getOrDefault(space.getId(), 0L),
                        messageCounts.getOrDefault(space.getId(), 0L)
                ))
                .toList();

        return new PagedResponse<>(
                content,
                spacesPage.getNumber(),
                spacesPage.getSize(),
                spacesPage.getTotalElements(),
                spacesPage.getTotalPages()
        );
    }

    @Transactional
    public GovernanceSpaceDto updateSpaceStatus(UUID spaceId, UpdateSpaceStatusRequest req) {
        UserEntity me = requireDirector();
        UUID orgId = me.getOrganization().getId();

        String targetStatus = req != null && req.status() != null ? req.status().trim().toUpperCase() : "";
        if (!VALID_SPACE_STATUSES.contains(targetStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado inválido. Valores permitidos: ACTIVE, FROZEN, ARCHIVED");
        }

        ChatConversationEntity space = conversationRepo.findById(spaceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Espacio no encontrado"));
        if (!orgId.equals(space.getOrganizationId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso denegado a esta organización");
        }
        if (!"SPACE".equalsIgnoreCase(space.getType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solo se puede cambiar el estado de conversaciones tipo SPACE");
        }

        String previousStatus = space.getStatus() != null ? space.getStatus() : "ACTIVE";
        space.setStatus(targetStatus);
        if ("ARCHIVED".equals(targetStatus)) {
            space.setArchivedAt(Instant.now());
            space.setArchivedBy(me.getId());
        } else {
            space.setArchivedAt(null);
            space.setArchivedBy(null);
        }

        ChatConversationEntity saved = conversationRepo.save(space);

        ChatSupportHelper.recordAudit(
                auditLogRepositoryPort,
                me,
                "CHAT_SPACE_STATUS_UPDATED",
                "CHAT_CONVERSATION",
                saved.getId(),
                List.of(
                        new ChatSupportHelper.AuditFieldChange("status", previousStatus, targetStatus),
                        new ChatSupportHelper.AuditFieldChange("spaceName", null, saved.getName())
                ),
                null
        );

        Map<UUID, Long> memberCounts = toCountMap(participantRepo.countActiveByConversationIds(List.of(saved.getId())));
        Map<UUID, Long> messageCounts = toCountMap(messageRepo.countByConversationIds(List.of(saved.getId())));

        return toSpaceDto(
                saved,
                memberCounts.getOrDefault(saved.getId(), 0L),
                messageCounts.getOrDefault(saved.getId(), 0L)
        );
    }

    @Transactional(readOnly = true)
    public PagedResponse<GovernanceChatUserDto> listUsers(String filter, String search, int page, int size) {
        UserEntity me = requireDirector();
        UUID orgId = me.getOrganization().getId();

        String normalizedFilter = (filter == null || filter.isBlank()) ? "ALL" : filter.trim().toUpperCase();
        if (!VALID_USER_FILTERS.contains(normalizedFilter)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Filtro inválido (ALL, CHAT_ONLY, WMS, FROZEN)");
        }

        Pageable pageable = buildPageable(page, size);
        Instant now = Instant.now();
        Page<UserEntity> usersPage = userRepo.findGovernanceUsers(
                orgId, normalizedFilter, toSearchPattern(search), now, pageable
        );

        List<UUID> userIds = usersPage.getContent().stream().map(UserEntity::getId).toList();
        Map<UUID, Long> spacesCountMap = userIds.isEmpty()
                ? Map.of()
                : toCountMap(participantRepo.countActiveSpacesByUserIds(userIds));

        Set<UUID> onlineIds = ChatSupportHelper.resolveOnlineUserIds(auditLogRepositoryPort);
        Set<String> wsUsernames = ChatSupportHelper.resolveOnlineWsUsernames(simpUserRegistry);

        List<GovernanceChatUserDto> content = usersPage.getContent().stream()
                .map(u -> {
                    String roleName = u.getRole() != null ? u.getRole().getName() : "USER";
                    boolean isChatOnly = "CHAT_ONLY".equalsIgnoreCase(roleName);
                    boolean enabled = Boolean.TRUE.equals(u.getIsEnabled());
                    boolean active = u.getStatus() == UserStatus.ACTIVE && enabled;
                    boolean online = active && (onlineIds.contains(u.getId()) || wsUsernames.contains(u.getUsername()));
                    return new GovernanceChatUserDto(
                            u.getId(),
                            u.getUsername(),
                            ChatSupportHelper.buildFullName(u),
                            u.getEmail(),
                            roleName,
                            isChatOnly,
                            enabled,
                            u.getStatus() != null ? u.getStatus().name() : UserStatus.PENDING.name(),
                            online,
                            u.getLastLogin() != null ? u.getLastLogin().toInstant() : null,
                            u.getChatFrozenUntil(),
                            u.getChatFrozenReason(),
                            spacesCountMap.getOrDefault(u.getId(), 0L)
                    );
                })
                .toList();

        return new PagedResponse<>(
                content,
                usersPage.getNumber(),
                usersPage.getSize(),
                usersPage.getTotalElements(),
                usersPage.getTotalPages()
        );
    }

    @Transactional
    public GovernanceActionResponse freezeUser(UUID targetUserId, FreezeUserRequest req) {
        UserEntity me = requireDirector();
        UUID orgId = me.getOrganization().getId();

        if (req == null || req.reason() == null || req.reason().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El motivo de congelamiento es obligatorio");
        }

        UserEntity target = userRepo.findById(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        if (target.getOrganization() == null || !orgId.equals(target.getOrganization().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso denegado a esta organización");
        }
        if (target.getId().equals(me.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No puedes congelar tu propia cuenta");
        }

        boolean chatOnly = req.chatOnlyFreeze() == null || Boolean.TRUE.equals(req.chatOnlyFreeze());
        Instant now = Instant.now();
        Instant frozenUntil = (req.durationHours() != null && req.durationHours() > 0)
                ? now.plus(req.durationHours(), ChronoUnit.HOURS)
                : now.plus(365, ChronoUnit.DAYS);
        String cleanReason = ChatSupportHelper.truncate(req.reason().trim(), 255);

        String prevEnabled = String.valueOf(target.getIsEnabled());
        String prevStatus = target.getStatus() != null ? target.getStatus().name() : null;
        String prevFrozenUntil = target.getChatFrozenUntil() != null ? target.getChatFrozenUntil().toString() : null;

        target.setChatFrozenUntil(frozenUntil);
        target.setChatFrozenReason(cleanReason);

        if (!chatOnly) {
            target.setIsEnabled(false);
            target.setStatus(UserStatus.SUSPENDED);
            tokenBlacklistPort.revokeUserSessions(target.getId());
        }

        userRepo.save(target);

        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("userId", target.getId());
            payload.put("username", target.getUsername());
            payload.put("chatOnlyFreeze", chatOnly);
            payload.put("chatFrozenUntil", frozenUntil);
            payload.put("reason", cleanReason);
            payload.put("accountSuspended", !chatOnly);

            ChatDtos.ChatWsEventDto wsEvent = new ChatDtos.ChatWsEventDto(
                    "CHAT_FROZEN",
                    null,
                    me.getId(),
                    me.getUsername(),
                    payload,
                    now
            );
            messagingTemplate.convertAndSendToUser(target.getUsername(), "/queue/chat", wsEvent);
        } catch (Exception ex) {
            log.warn("[CHAT-GOV] Error enviando notificación STOMP CHAT_FROZEN a {}: {}", target.getUsername(), ex.getMessage());
        }

        securityEventRepo.save(ChatSecurityEventEntity.builder()
                .organizationId(orgId)
                .branchId(target.getBranch() != null ? target.getBranch().getId() : null)
                .userId(target.getId())
                .eventType(chatOnly ? "CHAT_USER_FROZEN" : "ACCOUNT_SUSPENDED")
                .severity(chatOnly ? "HIGH" : "CRITICAL")
                .description("Congelamiento ejecutado por " + me.getUsername() + ": " + cleanReason)
                .ipAddress(ChatSupportHelper.extractClientIp(null, 64))
                .userAgent(ChatSupportHelper.extractUserAgent(null))
                .build());

        ChatSupportHelper.recordAudit(
                auditLogRepositoryPort,
                me,
                chatOnly ? "CHAT_USER_FROZEN" : "CHAT_USER_SUSPENDED",
                "USER",
                target.getId(),
                List.of(
                        new ChatSupportHelper.AuditFieldChange("chatFrozenUntil", prevFrozenUntil, frozenUntil.toString()),
                        new ChatSupportHelper.AuditFieldChange("chatFrozenReason", null, cleanReason),
                        new ChatSupportHelper.AuditFieldChange("chatOnlyFreeze", null, String.valueOf(chatOnly)),
                        new ChatSupportHelper.AuditFieldChange("isEnabled", prevEnabled, String.valueOf(target.getIsEnabled())),
                        new ChatSupportHelper.AuditFieldChange("status", prevStatus, target.getStatus() != null ? target.getStatus().name() : null)
                ),
                null
        );

        String message = chatOnly
                ? "Acceso al chat congelado para " + target.getUsername()
                : "Acceso al chat y cuenta WMS suspendidos para " + target.getUsername();
        return new GovernanceActionResponse(true, message);
    }

    @Transactional
    public GovernanceActionResponse unfreezeUser(UUID targetUserId) {
        UserEntity me = requireDirector();
        UUID orgId = me.getOrganization().getId();

        UserEntity target = userRepo.findById(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        if (target.getOrganization() == null || !orgId.equals(target.getOrganization().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso denegado a esta organización");
        }

        String prevEnabled = String.valueOf(target.getIsEnabled());
        String prevStatus = target.getStatus() != null ? target.getStatus().name() : null;
        Instant prevFrozenUntil = target.getChatFrozenUntil();
        String prevFrozenReason = target.getChatFrozenReason();

        target.setChatFrozenUntil(null);
        target.setChatFrozenReason(null);
        if (target.getStatus() == UserStatus.SUSPENDED) {
            target.setIsEnabled(true);
            target.setStatus(UserStatus.ACTIVE);
        }

        userRepo.save(target);

        ChatSupportHelper.recordAudit(
                auditLogRepositoryPort,
                me,
                "CHAT_USER_UNFROZEN",
                "USER",
                target.getId(),
                List.of(
                        new ChatSupportHelper.AuditFieldChange("chatFrozenUntil", prevFrozenUntil != null ? prevFrozenUntil.toString() : null, null),
                        new ChatSupportHelper.AuditFieldChange("chatFrozenReason", prevFrozenReason, null),
                        new ChatSupportHelper.AuditFieldChange("isEnabled", prevEnabled, String.valueOf(target.getIsEnabled())),
                        new ChatSupportHelper.AuditFieldChange("status", prevStatus, target.getStatus() != null ? target.getStatus().name() : null)
                ),
                null
        );

        return new GovernanceActionResponse(true, "Usuario descongelado correctamente");
    }

    @Transactional(readOnly = true)
    public PagedResponse<GovernanceAuditMessageDto> getAuditMessages(String type, int days, int page, int size) {
        UserEntity me = requireDirector();
        UUID orgId = me.getOrganization().getId();

        String normalizedType = (type == null || type.isBlank()) ? "ALL" : type.trim().toUpperCase();
        if (!VALID_AUDIT_MSG_TYPES.contains(normalizedType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de auditoría inválido (ALL, EDITED, DELETED)");
        }

        Pageable pageable = buildPageable(page, size);
        Instant since = Instant.now().minus(clampDays(days), ChronoUnit.DAYS);
        Page<ChatMessageEntity> msgPage = messageRepo.findGovernanceAuditMessages(orgId, normalizedType, since, pageable);

        List<ChatMessageEntity> msgs = msgPage.getContent();
        if (msgs.isEmpty()) {
            return new PagedResponse<>(List.of(), msgPage.getNumber(), msgPage.getSize(), msgPage.getTotalElements(), msgPage.getTotalPages());
        }

        Set<UUID> convIds = msgs.stream().map(ChatMessageEntity::getConversationId).collect(Collectors.toSet());
        Map<UUID, ChatConversationEntity> convMap = conversationRepo.findAllById(convIds).stream()
                .collect(Collectors.toMap(ChatConversationEntity::getId, c -> c));

        List<UUID> dmConvIds = convMap.values().stream()
                .filter(c -> "DM".equalsIgnoreCase(c.getType()) && (c.getName() == null || c.getName().isBlank()))
                .map(ChatConversationEntity::getId)
                .toList();
        Map<UUID, List<ChatParticipantEntity>> dmPartsByConv = dmConvIds.isEmpty()
                ? Map.of()
                : participantRepo.findByConversationIdInAndIsActiveTrue(dmConvIds).stream()
                        .collect(Collectors.groupingBy(ChatParticipantEntity::getConversationId));

        Set<UUID> userIds = msgs.stream().map(ChatMessageEntity::getSenderId).collect(Collectors.toSet());
        dmPartsByConv.values().forEach(list -> list.forEach(p -> userIds.add(p.getUserId())));
        Map<UUID, UserEntity> usersMap = userIds.isEmpty()
                ? Map.of()
                : userRepo.findAllById(userIds).stream().collect(Collectors.toMap(UserEntity::getId, u -> u));

        List<GovernanceAuditMessageDto> content = msgs.stream()
                .map(m -> {
                    ChatConversationEntity conv = convMap.get(m.getConversationId());
                    String convName = ChatSupportHelper.resolveConversationDisplayName(conv, dmPartsByConv, usersMap);
                    String convType = conv != null ? conv.getType() : "SPACE";
                    String senderName = ChatSupportHelper.buildFullName(usersMap.get(m.getSenderId()));
                    return new GovernanceAuditMessageDto(
                            m.getId(),
                            m.getConversationId(),
                            convName,
                            convType,
                            m.getSenderId(),
                            senderName,
                            m.getContent(),
                            m.getOriginalContent(),
                            Boolean.TRUE.equals(m.getIsEdited()),
                            m.getEditedAt(),
                            Boolean.TRUE.equals(m.getIsDeleted()),
                            m.getDeletedAt(),
                            m.getCreatedAt()
                    );
                })
                .toList();

        return new PagedResponse<>(
                content,
                msgPage.getNumber(),
                msgPage.getSize(),
                msgPage.getTotalElements(),
                msgPage.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public PagedResponse<GovernanceAttachmentDto> getAuditAttachments(int days, int page, int size) {
        UserEntity me = requireDirector();
        UUID orgId = me.getOrganization().getId();

        Pageable pageable = buildPageable(page, size);
        Instant since = Instant.now().minus(clampDays(days), ChronoUnit.DAYS);
        Page<AttachmentMetadataRow> attPage = attachmentRepo.findMetadataByOrganizationIdAndCreatedAtAfter(orgId, since, pageable);

        List<AttachmentMetadataRow> rows = attPage.getContent();
        if (rows.isEmpty()) {
            return new PagedResponse<>(List.of(), attPage.getNumber(), attPage.getSize(), attPage.getTotalElements(), attPage.getTotalPages());
        }

        List<UUID> dmConvIds = rows.stream()
                .filter(r -> "DM".equalsIgnoreCase(r.conversationType()) && (r.conversationName() == null || r.conversationName().isBlank()))
                .map(AttachmentMetadataRow::conversationId)
                .distinct()
                .toList();
        Map<UUID, List<ChatParticipantEntity>> dmPartsByConv = dmConvIds.isEmpty()
                ? Map.of()
                : participantRepo.findByConversationIdInAndIsActiveTrue(dmConvIds).stream()
                        .collect(Collectors.groupingBy(ChatParticipantEntity::getConversationId));

        Set<UUID> userIds = rows.stream().map(AttachmentMetadataRow::senderId).collect(Collectors.toSet());
        dmPartsByConv.values().forEach(list -> list.forEach(p -> userIds.add(p.getUserId())));
        Map<UUID, UserEntity> usersMap = userIds.isEmpty()
                ? Map.of()
                : userRepo.findAllById(userIds).stream().collect(Collectors.toMap(UserEntity::getId, u -> u));

        List<GovernanceAttachmentDto> content = rows.stream()
                .map(r -> {
                    String convName = r.conversationName();
                    if (convName == null || convName.isBlank()) {
                        if ("DM".equalsIgnoreCase(r.conversationType())) {
                            String names = dmPartsByConv.getOrDefault(r.conversationId(), List.of()).stream()
                                    .map(p -> usersMap.get(p.getUserId()))
                                    .filter(Objects::nonNull)
                                    .map(ChatSupportHelper::buildFullName)
                                    .distinct()
                                    .collect(Collectors.joining(" / "));
                            convName = names.isBlank() ? "Mensaje Directo" : "DM: " + names;
                        } else {
                            convName = "Espacio sin nombre";
                        }
                    }
                    return new GovernanceAttachmentDto(
                            r.id(),
                            r.messageId(),
                            r.conversationId(),
                            convName,
                            ChatSupportHelper.buildFullName(usersMap.get(r.senderId())),
                            r.fileName(),
                            r.contentType(),
                            r.fileSize() != null ? r.fileSize() : 0L,
                            r.createdAt()
                    );
                })
                .toList();

        return new PagedResponse<>(
                content,
                attPage.getNumber(),
                attPage.getSize(),
                attPage.getTotalElements(),
                attPage.getTotalPages()
        );
    }

    @Transactional
    public GovernanceAttachmentDownloadDto downloadAuditAttachment(UUID attachmentId) {
        UserEntity me = requireDirector();
        UUID orgId = me.getOrganization().getId();

        ChatAttachmentEntity att = attachmentRepo.findById(attachmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Adjunto no encontrado"));
        ChatMessageEntity msg = messageRepo.findById(att.getMessageId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mensaje del adjunto no encontrado"));
        ChatConversationEntity conv = conversationRepo.findById(msg.getConversationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversación no encontrada"));

        if (!orgId.equals(conv.getOrganizationId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso denegado a esta organización");
        }

        long fileSize = att.getFileSize() != null ? att.getFileSize() : 0L;
        ChatSupportHelper.recordAudit(
                auditLogRepositoryPort,
                me,
                "CHAT_ATTACHMENT_AUDITED_DOWNLOAD",
                "CHAT_ATTACHMENT",
                att.getId(),
                List.of(
                        new ChatSupportHelper.AuditFieldChange("fileName", null, att.getFileName()),
                        new ChatSupportHelper.AuditFieldChange("contentType", null, att.getContentType()),
                        new ChatSupportHelper.AuditFieldChange("fileSize", null, String.valueOf(fileSize)),
                        new ChatSupportHelper.AuditFieldChange("conversationId", null, conv.getId().toString()),
                        new ChatSupportHelper.AuditFieldChange("messageId", null, msg.getId().toString())
                ),
                null
        );

        return new GovernanceAttachmentDownloadDto(
                att.getId(),
                att.getFileName(),
                att.getContentType(),
                fileSize,
                att.getStorageUrl()
        );
    }

    @Transactional(readOnly = true)
    public PagedResponse<GovernanceDlpEventDto> getDlpEvents(int days, int page, int size) {
        UserEntity me = requireDirector();
        UUID orgId = me.getOrganization().getId();

        Pageable pageable = buildPageable(page, size);
        Instant since = Instant.now().minus(clampDays(days), ChronoUnit.DAYS);
        Page<ChatSecurityEventEntity> eventsPage = securityEventRepo
                .findByOrganizationIdAndCreatedAtAfterOrderByCreatedAtDesc(orgId, since, pageable);

        List<ChatSecurityEventEntity> events = eventsPage.getContent();
        if (events.isEmpty()) {
            return new PagedResponse<>(List.of(), eventsPage.getNumber(), eventsPage.getSize(), eventsPage.getTotalElements(), eventsPage.getTotalPages());
        }

        Set<UUID> convIds = events.stream()
                .map(ChatSecurityEventEntity::getConversationId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, ChatConversationEntity> convMap = convIds.isEmpty()
                ? Map.of()
                : conversationRepo.findAllById(convIds).stream()
                        .collect(Collectors.toMap(ChatConversationEntity::getId, c -> c));

        List<UUID> dmConvIds = convMap.values().stream()
                .filter(c -> "DM".equalsIgnoreCase(c.getType()) && (c.getName() == null || c.getName().isBlank()))
                .map(ChatConversationEntity::getId)
                .toList();
        Map<UUID, List<ChatParticipantEntity>> dmPartsByConv = dmConvIds.isEmpty()
                ? Map.of()
                : participantRepo.findByConversationIdInAndIsActiveTrue(dmConvIds).stream()
                        .collect(Collectors.groupingBy(ChatParticipantEntity::getConversationId));

        Set<UUID> userIds = events.stream()
                .map(ChatSecurityEventEntity::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        dmPartsByConv.values().forEach(list -> list.forEach(p -> userIds.add(p.getUserId())));
        Map<UUID, UserEntity> usersMap = userIds.isEmpty()
                ? Map.of()
                : userRepo.findAllById(userIds).stream().collect(Collectors.toMap(UserEntity::getId, u -> u));

        List<GovernanceDlpEventDto> content = events.stream()
                .map(e -> {
                    String convName = e.getConversationId() != null && convMap.containsKey(e.getConversationId())
                            ? ChatSupportHelper.resolveConversationDisplayName(convMap.get(e.getConversationId()), dmPartsByConv, usersMap)
                            : null;
                    String userName = e.getUserId() != null && usersMap.containsKey(e.getUserId())
                            ? ChatSupportHelper.buildFullName(usersMap.get(e.getUserId()))
                            : null;
                    return new GovernanceDlpEventDto(
                            e.getId(),
                            e.getConversationId(),
                            convName,
                            e.getUserId(),
                            userName,
                            e.getEventType(),
                            e.getSeverity(),
                            e.getDescription(),
                            e.getIpAddress(),
                            e.getCreatedAt()
                    );
                })
                .toList();

        return new PagedResponse<>(
                content,
                eventsPage.getNumber(),
                eventsPage.getSize(),
                eventsPage.getTotalElements(),
                eventsPage.getTotalPages()
        );
    }

    @Transactional
    public DlpReportResponse reportDlpEvent(ReportDlpEventRequest req, HttpServletRequest httpRequest) {
        UserEntity me = getCurrentUser();
        UUID orgId = me.getOrganization().getId();

        if (req == null || req.eventType() == null || req.eventType().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El tipo de evento DLP es obligatorio");
        }

        UUID branchId = me.getBranch() != null ? me.getBranch().getId() : null;
        if (req.conversationId() != null) {
            ChatConversationEntity conv = conversationRepo.findById(req.conversationId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversación no encontrada"));
            if (!orgId.equals(conv.getOrganizationId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso denegado a esta organización");
            }
            if (branchId == null) {
                branchId = conv.getBranchId();
            }
        }

        String eventType = ChatSupportHelper.truncate(req.eventType().trim().toUpperCase(), 50);
        String severity = (eventType.contains("SCREENSHOT") || eventType.contains("SENSITIVE") || eventType.contains("EXFIL") || eventType.contains("LEAK"))
                ? "HIGH"
                : "MEDIUM";

        securityEventRepo.save(ChatSecurityEventEntity.builder()
                .organizationId(orgId)
                .branchId(branchId)
                .conversationId(req.conversationId())
                .userId(me.getId())
                .eventType(eventType)
                .severity(severity)
                .description(req.description() != null ? req.description().trim() : null)
                .ipAddress(ChatSupportHelper.extractClientIp(httpRequest, 64))
                .userAgent(ChatSupportHelper.extractUserAgent(httpRequest))
                .build());

        return new DlpReportResponse(true);
    }

    @Transactional
    public BreakGlassTranscriptDto breakGlassInspect(UUID conversationId, BreakGlassRequest req) {
        UserEntity me = requireDirector();
        UUID orgId = me.getOrganization().getId();

        if (req == null || req.reason() == null || req.reason().trim().length() < 10) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El motivo de acceso Break-Glass es obligatorio (mínimo 10 caracteres)"
            );
        }

        ChatConversationEntity conv = conversationRepo.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversación no encontrada"));
        if (!orgId.equals(conv.getOrganizationId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso denegado a esta organización");
        }

        String cleanReason = req.reason().trim();
        Instant auditedAt = Instant.now();

        List<ChatMessageEntity> lastMessages = new ArrayList<>(
                messageRepo.findByConversationIdOrderByCreatedAtDesc(conv.getId(), PageRequest.of(0, MAX_PAGE_SIZE))
        );
        Collections.reverse(lastMessages);

        List<UUID> msgIds = lastMessages.stream().map(ChatMessageEntity::getId).toList();
        Map<UUID, Long> attachmentCounts = msgIds.isEmpty()
                ? Map.of()
                : toCountMap(attachmentRepo.countByMessageIdIn(msgIds));

        Map<UUID, List<ChatParticipantEntity>> dmPartsByConv = "DM".equalsIgnoreCase(conv.getType())
                ? Map.of(conv.getId(), participantRepo.findByConversationIdInAndIsActiveTrue(List.of(conv.getId())))
                : Map.of();

        Set<UUID> userIds = lastMessages.stream().map(ChatMessageEntity::getSenderId).collect(Collectors.toSet());
        dmPartsByConv.values().forEach(list -> list.forEach(p -> userIds.add(p.getUserId())));
        Map<UUID, UserEntity> usersMap = userIds.isEmpty()
                ? Map.of()
                : userRepo.findAllById(userIds).stream().collect(Collectors.toMap(UserEntity::getId, u -> u));

        String conversationName = ChatSupportHelper.resolveConversationDisplayName(conv, dmPartsByConv, usersMap);

        ChatSupportHelper.recordAudit(
                auditLogRepositoryPort,
                me,
                "BREAK_GLASS_ACCESS",
                "CHAT_CONVERSATION",
                conv.getId(),
                List.of(
                        new ChatSupportHelper.AuditFieldChange("reason", null, cleanReason),
                        new ChatSupportHelper.AuditFieldChange("conversationType", null, conv.getType()),
                        new ChatSupportHelper.AuditFieldChange("conversationName", null, conversationName),
                        new ChatSupportHelper.AuditFieldChange("messagesInspected", null, String.valueOf(lastMessages.size()))
                ),
                null
        );

        securityEventRepo.save(ChatSecurityEventEntity.builder()
                .organizationId(orgId)
                .branchId(conv.getBranchId())
                .conversationId(conv.getId())
                .userId(me.getId())
                .eventType("BREAK_GLASS_ACCESS")
                .severity("HIGH")
                .description("Acceso Break-Glass por " + me.getUsername() + ": " + cleanReason)
                .ipAddress(ChatSupportHelper.extractClientIp(null, 64))
                .userAgent(ChatSupportHelper.extractUserAgent(null))
                .build());

        List<BreakGlassMessageDto> messageDtos = lastMessages.stream()
                .map(m -> new BreakGlassMessageDto(
                        m.getId(),
                        ChatSupportHelper.buildFullName(usersMap.get(m.getSenderId())),
                        m.getContent(),
                        m.getOriginalContent(),
                        Boolean.TRUE.equals(m.getIsEdited()),
                        Boolean.TRUE.equals(m.getIsDeleted()),
                        m.getCreatedAt(),
                        attachmentCounts.getOrDefault(m.getId(), 0L)
                ))
                .toList();

        return new BreakGlassTranscriptDto(
                conv.getId(),
                conversationName,
                conv.getType(),
                auditedAt,
                cleanReason,
                messageDtos
        );
    }

    private GovernanceSpaceDto toSpaceDto(ChatConversationEntity space, long memberCount, long messageCount) {
        return new GovernanceSpaceDto(
                space.getId(),
                space.getName(),
                space.getDescription(),
                space.getCategory(),
                space.getStatus() != null ? space.getStatus() : "ACTIVE",
                space.getBranchId(),
                memberCount,
                messageCount,
                space.getLastMessageAt(),
                space.getCreatedAt()
        );
    }

    private Map<UUID, Long> toCountMap(List<Object[]> rows) {
        if (rows == null || rows.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            if (row != null && row.length >= 2 && row[0] instanceof UUID id && row[1] instanceof Number num) {
                map.put(id, num.longValue());
            }
        }
        return map;
    }
}
