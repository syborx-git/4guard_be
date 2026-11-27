package com.fourguard.wms.application.usecase;

import com.fourguard.wms.application.dto.chat.ChatDtos.*;
import com.fourguard.wms.domain.enums.UserStatus;
import com.fourguard.wms.domain.ports.out.AuditLogRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.OrganizationEntity;
import com.fourguard.wms.infrastructure.persistence.entity.RoleEntity;
import com.fourguard.wms.infrastructure.persistence.entity.UserEntity;
import com.fourguard.wms.infrastructure.persistence.entity.chat.*;
import com.fourguard.wms.infrastructure.persistence.repository.UserJpaRepository;
import com.fourguard.wms.infrastructure.persistence.repository.chat.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceSecurityAndConcurrencyTest {

    @Mock private ChatConversationRepository conversationRepo;
    @Mock private ChatParticipantRepository participantRepo;
    @Mock private ChatMessageRepository messageRepo;
    @Mock private ChatMessageReactionRepository reactionRepo;
    @Mock private ChatAttachmentRepository attachmentRepo;
    @Mock private UserJpaRepository userRepo;
    @Mock private AuditLogRepositoryPort auditLogRepositoryPort;
    @Mock private SimpMessagingTemplate messagingTemplate;

    private ChatService chatService;

    private UUID orgAId;
    private UUID orgBId;
    private OrganizationEntity orgA;
    private OrganizationEntity orgB;
    private UserEntity userA;
    private UserEntity userA2;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(
                conversationRepo,
                participantRepo,
                messageRepo,
                reactionRepo,
                attachmentRepo,
                userRepo,
                auditLogRepositoryPort,
                messagingTemplate,
                Optional.empty()
        );

        orgAId = UUID.randomUUID();
        orgBId = UUID.randomUUID();
        orgA = OrganizationEntity.builder().id(orgAId).name("Org A").code("ORGA").build();
        orgB = OrganizationEntity.builder().id(orgBId).name("Org B").code("ORGB").build();

        userA = buildUser(UUID.randomUUID(), "alice", "Alice", "Admin", orgA, "CEO");
        userA2 = buildUser(UUID.randomUUID(), "bob", "Bob", "Ops", orgA, "OPERATIONS_MANAGER");

        authenticateAs(userA.getUsername());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, "N/A", List.of())
        );
    }

    private UserEntity buildUser(UUID id, String username, String firstName, String lastName,
                                 OrganizationEntity org, String roleName) {
        return UserEntity.builder()
                .id(id)
                .username(username)
                .email(username + "@4guard.local")
                .firstName(firstName)
                .lastName(lastName)
                .organization(org)
                .role(RoleEntity.builder().id(UUID.randomUUID()).name(roleName).level(1).build())
                .status(UserStatus.ACTIVE)
                .isEnabled(true)
                .build();
    }

    // 1. Concurrency: getOrCreateDm with 10 concurrent threads creates only 1 DM
    @Test
    void getOrCreateDm_concurrentRequests_createsOnlyOneConversation() throws Exception {
        AtomicReference<ChatConversationEntity> stored = new AtomicReference<>();
        AtomicInteger saveCount = new AtomicInteger(0);

        when(userRepo.findByUsername(userA.getUsername())).thenReturn(Optional.of(userA));
        when(userRepo.findById(userA2.getId())).thenReturn(Optional.of(userA2));
        when(conversationRepo.findDirectMessageBetween(orgAId, userA.getId(), userA2.getId()))
                .thenAnswer(inv -> Optional.ofNullable(stored.get()));
        when(conversationRepo.save(any(ChatConversationEntity.class))).thenAnswer(inv -> {
            Thread.sleep(15); // widen race window to verify lock actually serializes creation
            ChatConversationEntity entity = inv.getArgument(0);
            if (entity.getId() == null) {
                entity.setId(UUID.randomUUID());
            }
            saveCount.incrementAndGet();
            stored.set(entity);
            return entity;
        });
        when(participantRepo.save(any(ChatParticipantEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(participantRepo.findByConversationIdAndIsActiveTrue(any())).thenAnswer(inv -> List.of(
                ChatParticipantEntity.builder().conversationId(inv.getArgument(0)).userId(userA.getId()).role("MEMBER").isActive(true).build(),
                ChatParticipantEntity.builder().conversationId(inv.getArgument(0)).userId(userA2.getId()).role("MEMBER").isActive(true).build()
        ));
        when(auditLogRepositoryPort.findByActionAndCreatedAtAfter(eq("LOGIN"), any())).thenReturn(List.of());
        when(messageRepo.countByConversationIdAndCreatedAtAfterAndSenderIdNot(any(), any(), any())).thenReturn(0L);
        when(messageRepo.findTopByConversationIdOrderByCreatedAtDesc(any())).thenReturn(Optional.empty());

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<ConversationSummaryDto>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                authenticateAs(userA.getUsername());
                ready.countDown();
                start.await();
                try {
                    return chatService.getOrCreateDm(new CreateDmRequest(userA2.getId()));
                } finally {
                    SecurityContextHolder.clearContext();
                }
            }));
        }

        assertTrue(ready.await(5, TimeUnit.SECONDS));
        start.countDown();

        Set<UUID> returnedIds = new HashSet<>();
        for (Future<ConversationSummaryDto> f : futures) {
            ConversationSummaryDto dto = f.get(10, TimeUnit.SECONDS);
            assertNotNull(dto.id());
            returnedIds.add(dto.id());
        }
        executor.shutdownNow();

        assertEquals(1, saveCount.get(), "Exactly 1 DM conversation must be created under 10 concurrent requests");
        assertEquals(1, returnedIds.size(), "All 10 threads must receive the same conversation ID");
    }

    // 2. Concurrency: toggleReaction handles DataIntegrityViolationException gracefully
    @Test
    void toggleReaction_concurrentDuplicateInsert_handledGracefullyWithout500() {
        UUID convId = UUID.randomUUID();
        UUID msgId = UUID.randomUUID();
        ChatMessageEntity msg = ChatMessageEntity.builder()
                .id(msgId)
                .conversationId(convId)
                .senderId(userA.getId())
                .content("Hola")
                .messageType("TEXT")
                .createdAt(Instant.now())
                .build();

        when(userRepo.findByUsername(userA.getUsername())).thenReturn(Optional.of(userA));
        when(messageRepo.findById(msgId)).thenReturn(Optional.of(msg));
        when(participantRepo.existsByConversationIdAndUserIdAndIsActiveTrue(convId, userA.getId())).thenReturn(true);
        when(reactionRepo.findByMessageIdAndUserIdAndEmoji(msgId, userA.getId(), "👍")).thenReturn(Optional.empty());
        when(reactionRepo.save(any(ChatMessageReactionEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));
        when(reactionRepo.findByMessageIdIn(List.of(msgId))).thenReturn(List.of());
        when(attachmentRepo.findByMessageIdIn(List.of(msgId))).thenReturn(List.of());
        when(userRepo.findAllById(any())).thenReturn(List.of(userA));
        when(participantRepo.findByConversationIdAndIsActiveTrue(convId)).thenReturn(List.of());

        MessageDto dto = assertDoesNotThrow(() ->
                chatService.toggleReaction(msgId, new ToggleReactionRequest("👍"))
        );
        assertNotNull(dto);
        assertEquals(msgId, dto.id());
    }

    // 3. Monotonic markAsRead: lastReadAt never moves backwards
    @Test
    void markAsRead_existingNewerLastReadAt_neverMovesBackwards() {
        UUID convId = UUID.randomUUID();
        Instant futureTimestamp = Instant.now().plusSeconds(120);
        ChatConversationEntity conv = ChatConversationEntity.builder()
                .id(convId).organizationId(orgAId).type("DM").build();
        ChatParticipantEntity participant = ChatParticipantEntity.builder()
                .id(UUID.randomUUID())
                .conversationId(convId)
                .userId(userA.getId())
                .isActive(true)
                .lastReadAt(futureTimestamp)
                .build();

        when(userRepo.findByUsername(userA.getUsername())).thenReturn(Optional.of(userA));
        when(conversationRepo.findById(convId)).thenReturn(Optional.of(conv));
        when(participantRepo.existsByConversationIdAndUserIdAndIsActiveTrue(convId, userA.getId())).thenReturn(true);
        when(participantRepo.findByConversationIdAndUserId(convId, userA.getId())).thenReturn(Optional.of(participant));
        when(messageRepo.findTopByConversationIdOrderByCreatedAtDesc(convId)).thenReturn(Optional.empty());

        chatService.markAsRead(convId);

        assertEquals(futureTimestamp, participant.getLastReadAt(),
                "lastReadAt must remain monotonic and never move backwards");
        verify(participantRepo).save(participant);
    }

    // 4. N+1 Bound: listMyConversations with 5 conversations queries auditLogRepositoryPort at most ONCE
    @Test
    void listMyConversations_fiveConversations_queriesOnlineAuditLogAtMostOnce() {
        when(userRepo.findByUsername(userA.getUsername())).thenReturn(Optional.of(userA));
        List<ChatConversationEntity> fiveConvs = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            UUID cid = UUID.randomUUID();
            fiveConvs.add(ChatConversationEntity.builder()
                    .id(cid).organizationId(orgAId).type("SPACE").name("Space " + i).build());
            when(participantRepo.findByConversationIdAndIsActiveTrue(cid)).thenReturn(List.of(
                    ChatParticipantEntity.builder().conversationId(cid).userId(userA.getId()).role("OWNER").isActive(true).lastReadAt(Instant.EPOCH).build()
            ));
        }
        when(conversationRepo.findActiveByOrganizationAndUser(orgAId, userA.getId())).thenReturn(fiveConvs);
        when(auditLogRepositoryPort.findByActionAndCreatedAtAfter(eq("LOGIN"), any())).thenReturn(List.of());
        when(messageRepo.countByConversationIdAndCreatedAtAfterAndSenderIdNot(any(), any(), any())).thenReturn(0L);
        when(messageRepo.findTopByConversationIdOrderByCreatedAtDesc(any())).thenReturn(Optional.empty());

        List<ConversationSummaryDto> result = chatService.listMyConversations();

        assertEquals(5, result.size());
        verify(auditLogRepositoryPort, times(1)).findByActionAndCreatedAtAfter(eq("LOGIN"), any());
    }

    private UUID stubSendMessageContext(UUID convId) {
        ChatConversationEntity conv = ChatConversationEntity.builder()
                .id(convId).organizationId(orgAId).type("SPACE").name("Ops").build();
        when(userRepo.findByUsername(userA.getUsername())).thenReturn(Optional.of(userA));
        when(conversationRepo.findById(convId)).thenReturn(Optional.of(conv));
        when(participantRepo.existsByConversationIdAndUserIdAndIsActiveTrue(convId, userA.getId())).thenReturn(true);
        when(messageRepo.save(any(ChatMessageEntity.class))).thenAnswer(inv -> {
            ChatMessageEntity m = inv.getArgument(0);
            m.setId(UUID.randomUUID());
            m.setCreatedAt(Instant.now());
            return m;
        });
        return convId;
    }

    // 5. Attachment Integrity: Corrupted Base64 rejected with 400 BAD_REQUEST
    @Test
    void sendMessage_corruptedBase64Attachment_rejectedWith400() {
        UUID convId = stubSendMessageContext(UUID.randomUUID());
        AttachmentInput badAtt = new AttachmentInput(
                "broken.png",
                "image/png",
                128L,
                "data:image/png;base64,!!!NOT-VALID-BASE64!!!"
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                chatService.sendMessage(convId, new SendMessageRequest("adjunto", null, "TEXT", null, List.of(badAtt)))
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(attachmentRepo, never()).save(any());
    }

    // 6. Attachment Integrity: Magic-Byte Spoofing rejected with 400 BAD_REQUEST
    @Test
    void sendMessage_magicByteSpoofing_rejectedWith400() {
        UUID convId = stubSendMessageContext(UUID.randomUUID());

        // Case A: PDF bytes disguised as image/png
        String pdfBytesAsB64 = Base64.getEncoder().encodeToString("%PDF-1.7\nfake".getBytes(StandardCharsets.US_ASCII));
        AttachmentInput pdfAsPng = new AttachmentInput(
                "spoof.png",
                "image/png",
                pdfBytesAsB64.length(),
                "data:image/png;base64," + pdfBytesAsB64
        );
        ResponseStatusException ex1 = assertThrows(ResponseStatusException.class, () ->
                chatService.sendMessage(convId, new SendMessageRequest("spoof1", null, "TEXT", null, List.of(pdfAsPng)))
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex1.getStatusCode());

        // Case B: HTML/XSS bytes disguised as image/jpeg
        String htmlAsB64 = Base64.getEncoder().encodeToString("<html><script>alert(1)</script></html>".getBytes(StandardCharsets.UTF_8));
        AttachmentInput htmlAsJpeg = new AttachmentInput(
                "xss.jpg",
                "image/jpeg",
                htmlAsB64.length(),
                "data:image/jpeg;base64," + htmlAsB64
        );
        ResponseStatusException ex2 = assertThrows(ResponseStatusException.class, () ->
                chatService.sendMessage(convId, new SendMessageRequest("spoof2", null, "TEXT", null, List.of(htmlAsJpeg)))
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex2.getStatusCode());
        verify(attachmentRepo, never()).save(any());
    }

    // 7. Attachment Integrity: Oversized > 2MB & XSS image/svg+xml rejected with 400 BAD_REQUEST
    @Test
    void sendMessage_oversizedAndSvgAttachments_rejectedWith400() {
        UUID convId = stubSendMessageContext(UUID.randomUUID());

        AttachmentInput oversized = new AttachmentInput(
                "huge.png",
                "image/png",
                2L * 1024 * 1024 + 1,
                "data:image/png;base64,iVBORw0KGgo="
        );
        ResponseStatusException ex1 = assertThrows(ResponseStatusException.class, () ->
                chatService.sendMessage(convId, new SendMessageRequest("big", null, "TEXT", null, List.of(oversized)))
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex1.getStatusCode());

        String svgB64 = Base64.getEncoder().encodeToString("<svg onload='alert(1)'/>".getBytes(StandardCharsets.UTF_8));
        AttachmentInput svgXss = new AttachmentInput(
                "evil.svg",
                "image/svg+xml",
                100L,
                "data:image/svg+xml;base64," + svgB64
        );
        ResponseStatusException ex2 = assertThrows(ResponseStatusException.class, () ->
                chatService.sendMessage(convId, new SendMessageRequest("svg", null, "TEXT", null, List.of(svgXss)))
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex2.getStatusCode());
        verify(attachmentRepo, never()).save(any());
    }

    // 8. Attachment Integrity (Valid PNG & Valid PDF) + WebSocket Buffer Safety
    @Test
    void sendMessage_validPngAndPdf_persistedAndStrippedInWsEventOnly() {
        UUID convId = stubSendMessageContext(UUID.randomUUID());
        List<ChatAttachmentEntity> savedAttachments = new ArrayList<>();

        when(attachmentRepo.save(any(ChatAttachmentEntity.class))).thenAnswer(inv -> {
            ChatAttachmentEntity a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            savedAttachments.add(a);
            return a;
        });
        when(conversationRepo.save(any(ChatConversationEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(participantRepo.findByConversationIdAndUserId(convId, userA.getId())).thenReturn(Optional.empty());
        when(reactionRepo.findByMessageIdIn(anyList())).thenReturn(List.of());
        when(attachmentRepo.findByMessageIdIn(anyList())).thenAnswer(inv -> List.copyOf(savedAttachments));
        when(userRepo.findAllById(any())).thenReturn(List.of(userA));
        when(participantRepo.findByConversationIdAndIsActiveTrue(convId)).thenReturn(List.of());

        byte[] pngBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        String pngDataUrl = "data:image/png;base64," + Base64.getEncoder().encodeToString(pngBytes);

        byte[] pdfBytes = "%PDF-1.4\n1 0 obj\n<<>>\nendobj\n".getBytes(StandardCharsets.US_ASCII);
        String pdfDataUrl = "data:application/pdf;base64," + Base64.getEncoder().encodeToString(pdfBytes);

        AttachmentInput pngAtt = new AttachmentInput("evidencia.png", "image/png", pngBytes.length, pngDataUrl);
        AttachmentInput pdfAtt = new AttachmentInput("remision.pdf", "application/pdf", pdfBytes.length, pdfDataUrl);

        MessageDto restDto = chatService.sendMessage(
                convId,
                new SendMessageRequest("Evidencias adjuntas", null, "TEXT", null, List.of(pngAtt, pdfAtt))
        );

        assertEquals(2, savedAttachments.size());
        assertEquals(2, restDto.attachments().size());
        assertEquals(pngDataUrl, restDto.attachments().get(0).storageUrl(), "REST response must retain full PNG data URL");
        assertEquals(pdfDataUrl, restDto.attachments().get(1).storageUrl(), "REST response must retain full PDF data URL");

        ArgumentCaptor<ChatWsEventDto> wsCaptor = ArgumentCaptor.forClass(ChatWsEventDto.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/chat." + convId), wsCaptor.capture());

        ChatWsEventDto wsEvent = wsCaptor.getValue();
        assertInstanceOf(MessageDto.class, wsEvent.payload());
        MessageDto wsMsg = (MessageDto) wsEvent.payload();
        assertEquals(2, wsMsg.attachments().size());
        assertEquals("", wsMsg.attachments().get(0).storageUrl(), "WS event must strip inline data: URL for PNG");
        assertEquals("evidencia.png", wsMsg.attachments().get(0).fileName());
        assertEquals("", wsMsg.attachments().get(1).storageUrl(), "WS event must strip inline data: URL for PDF");
        assertEquals("remision.pdf", wsMsg.attachments().get(1).fileName());
    }

    // 9. Multi-Tenant Isolation: Org A user gets 403 FORBIDDEN on Org B conversation
    @Test
    void multiTenantIsolation_orgAUserDeniedOnOrgBConversation() {
        UUID orgBConvId = UUID.randomUUID();
        UUID orgBMsgId = UUID.randomUUID();
        ChatConversationEntity orgBConv = ChatConversationEntity.builder()
                .id(orgBConvId).organizationId(orgBId).type("SPACE").name("Org B Secret").build();
        ChatMessageEntity orgBMsg = ChatMessageEntity.builder()
                .id(orgBMsgId).conversationId(orgBConvId).senderId(UUID.randomUUID()).content("Secret").build();

        when(userRepo.findByUsername(userA.getUsername())).thenReturn(Optional.of(userA));
        when(conversationRepo.findById(orgBConvId)).thenReturn(Optional.of(orgBConv));
        when(messageRepo.findById(orgBMsgId)).thenReturn(Optional.of(orgBMsg));

        ResponseStatusException sendEx = assertThrows(ResponseStatusException.class, () ->
                chatService.sendMessage(orgBConvId, new SendMessageRequest("intrusion", null, "TEXT", null, null))
        );
        assertEquals(HttpStatus.FORBIDDEN, sendEx.getStatusCode());

        ResponseStatusException deleteEx = assertThrows(ResponseStatusException.class, () ->
                chatService.deleteMessage(orgBMsgId)
        );
        assertEquals(HttpStatus.FORBIDDEN, deleteEx.getStatusCode());

        ResponseStatusException pinEx = assertThrows(ResponseStatusException.class, () ->
                chatService.togglePinMessage(orgBMsgId, true)
        );
        assertEquals(HttpStatus.FORBIDDEN, pinEx.getStatusCode());
    }

    // 10. Director-Only RBAC: OPERATIONS_MANAGER gets 403 on createSpace; CEO succeeds
    @Test
    void createSpace_directorOnlyRbac_blocksOperationsManagerAndAllowsCeo() {
        CreateSpaceRequest req = new CreateSpaceRequest(
                "Comité Directivo", "Solo dirección", "shield", "DIRECTIVO", null, List.of(userA2.getId())
        );

        // OPERATIONS_MANAGER -> 403 FORBIDDEN
        authenticateAs(userA2.getUsername());
        when(userRepo.findByUsername(userA2.getUsername())).thenReturn(Optional.of(userA2));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                chatService.createSpace(req)
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(conversationRepo, never()).save(any());

        // CEO -> succeeds and batch-fetches users via findAllById
        authenticateAs(userA.getUsername());
        when(userRepo.findByUsername(userA.getUsername())).thenReturn(Optional.of(userA));
        when(conversationRepo.save(any(ChatConversationEntity.class))).thenAnswer(inv -> {
            ChatConversationEntity e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });
        when(userRepo.findAllById(any())).thenReturn(List.of(userA, userA2));
        when(participantRepo.save(any(ChatParticipantEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(participantRepo.findByConversationIdAndIsActiveTrue(any())).thenReturn(List.of(
                ChatParticipantEntity.builder().userId(userA.getId()).role("OWNER").isActive(true).lastReadAt(Instant.EPOCH).build(),
                ChatParticipantEntity.builder().userId(userA2.getId()).role("MEMBER").isActive(true).lastReadAt(Instant.EPOCH).build()
        ));
        when(auditLogRepositoryPort.findByActionAndCreatedAtAfter(eq("LOGIN"), any())).thenReturn(List.of());
        when(messageRepo.countByConversationIdAndCreatedAtAfterAndSenderIdNot(any(), any(), any())).thenReturn(0L);
        when(messageRepo.findTopByConversationIdOrderByCreatedAtDesc(any())).thenReturn(Optional.empty());

        ConversationSummaryDto created = chatService.createSpace(req);

        assertNotNull(created.id());
        assertEquals("Comité Directivo", created.name());
        assertEquals("SPACE", created.type());
        verify(userRepo, times(1)).findAllById(any());
        verify(userRepo, never()).findById(any());
    }
}
