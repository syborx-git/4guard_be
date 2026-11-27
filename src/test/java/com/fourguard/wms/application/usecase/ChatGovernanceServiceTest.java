package com.fourguard.wms.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fourguard.wms.application.dto.chat.AttachmentMetadataRow;
import com.fourguard.wms.application.dto.chat.ChatDtos.*;
import com.fourguard.wms.application.dto.chat.ChatGovernanceDtos.*;
import com.fourguard.wms.application.dto.request.auth.RefreshTokenRequest;
import com.fourguard.wms.application.mapper.UserMapper;
import com.fourguard.wms.domain.enums.UserStatus;
import com.fourguard.wms.domain.exception.InvalidCredentialsException;
import com.fourguard.wms.domain.ports.out.AuditLogRepositoryPort;
import com.fourguard.wms.domain.ports.out.ForkliftOperatorRepositoryPort;
import com.fourguard.wms.domain.ports.out.TokenBlacklistPort;
import com.fourguard.wms.domain.ports.out.UserRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.AuditLogEntity;
import com.fourguard.wms.infrastructure.persistence.entity.OrganizationEntity;
import com.fourguard.wms.infrastructure.persistence.entity.RoleEntity;
import com.fourguard.wms.infrastructure.persistence.entity.UserEntity;
import com.fourguard.wms.infrastructure.persistence.entity.chat.*;
import com.fourguard.wms.infrastructure.persistence.repository.UserJpaRepository;
import com.fourguard.wms.infrastructure.persistence.repository.chat.*;
import com.fourguard.wms.infrastructure.security.UserDetailsServiceImpl;
import com.fourguard.wms.infrastructure.security.jwt.JwtProperties;
import com.fourguard.wms.infrastructure.security.jwt.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatGovernanceServiceTest {

    @Mock private ChatConversationRepository conversationRepo;
    @Mock private ChatParticipantRepository participantRepo;
    @Mock private ChatMessageRepository messageRepo;
    @Mock private ChatMessageReactionRepository reactionRepo;
    @Mock private ChatAttachmentRepository attachmentRepo;
    @Mock private ChatSecurityEventJpaRepository securityEventRepo;
    @Mock private UserJpaRepository userRepo;
    @Mock private AuditLogRepositoryPort auditLogRepositoryPort;
    @Mock private TokenBlacklistPort tokenBlacklistPort;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @Mock private UserRepositoryPort userRepositoryPort;
    @Mock private ForkliftOperatorRepositoryPort forkliftOperatorRepositoryPort;
    @Mock private JwtService jwtService;
    @Mock private JwtProperties jwtProperties;
    @Mock private UserMapper userMapper;

    private ChatGovernanceService governanceService;
    private ChatService chatService;
    private RefreshTokenUseCaseImpl refreshTokenUseCase;
    private UserDetailsServiceImpl userDetailsService;

    private UUID orgId;
    private UUID otherOrgId;
    private OrganizationEntity org;
    private OrganizationEntity otherOrg;
    private UserEntity ceoUser;
    private UserEntity directorUser;
    private UserEntity managerUser;
    private UserEntity chatOnlyUser;

    @BeforeEach
    void setUp() {
        governanceService = new ChatGovernanceService(
                conversationRepo,
                participantRepo,
                messageRepo,
                attachmentRepo,
                securityEventRepo,
                userRepo,
                auditLogRepositoryPort,
                tokenBlacklistPort,
                messagingTemplate,
                Optional.empty()
        );

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

        refreshTokenUseCase = new RefreshTokenUseCaseImpl(
                userRepositoryPort,
                forkliftOperatorRepositoryPort,
                tokenBlacklistPort,
                jwtService,
                jwtProperties,
                userMapper
        );

        userDetailsService = new UserDetailsServiceImpl(userRepositoryPort);

        orgId = UUID.randomUUID();
        otherOrgId = UUID.randomUUID();
        org = OrganizationEntity.builder().id(orgId).name("Org Principal").code("ORG1").build();
        otherOrg = OrganizationEntity.builder().id(otherOrgId).name("Org Ajena").code("ORG2").build();

        ceoUser = buildUser(UUID.randomUUID(), "ceo.elena", "Elena", "Vega", org, "CEO");
        directorUser = buildUser(UUID.randomUUID(), "dir.carlos", "Carlos", "Mendoza", org, "DIRECTOR");
        managerUser = buildUser(UUID.randomUUID(), "mgr.luis", "Luis", "Torres", org, "OPERATIONS_MANAGER");
        chatOnlyUser = buildUser(UUID.randomUUID(), "chat.sofia", "Sofia", "Ruiz", org, "CHAT_ONLY");

        authenticateAs(ceoUser.getUsername());
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
                                 OrganizationEntity organization, String roleName) {
        return UserEntity.builder()
                .id(id)
                .username(username)
                .email(username + "@4guard.local")
                .password("encoded")
                .firstName(firstName)
                .lastName(lastName)
                .organization(organization)
                .role(RoleEntity.builder().id(UUID.randomUUID()).name(roleName).level(1).permissions(Set.of()).build())
                .status(UserStatus.ACTIVE)
                .isEnabled(true)
                .build();
    }

    @Test
    void freezeAndUnfreeze_chatOnlyFreeze_keepsWmsEnabledAndSendsStompAndAudits() {
        when(userRepo.findByUsername(ceoUser.getUsername())).thenReturn(Optional.of(ceoUser));
        when(userRepo.findById(chatOnlyUser.getId())).thenReturn(Optional.of(chatOnlyUser));
        when(userRepo.save(any(UserEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(securityEventRepo.save(any(ChatSecurityEventEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(auditLogRepositoryPort.log(any(AuditLogEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        GovernanceActionResponse freezeRes = governanceService.freezeUser(
                chatOnlyUser.getId(),
                new FreezeUserRequest("Conducta inapropiada en sala", true, 24)
        );

        assertTrue(freezeRes.success());
        assertNotNull(chatOnlyUser.getChatFrozenUntil());
        assertTrue(chatOnlyUser.getChatFrozenUntil().isAfter(Instant.now().plus(23, ChronoUnit.HOURS)));
        assertEquals("Conducta inapropiada en sala", chatOnlyUser.getChatFrozenReason());
        assertTrue(chatOnlyUser.getIsEnabled());
        assertEquals(UserStatus.ACTIVE, chatOnlyUser.getStatus());

        verify(tokenBlacklistPort, never()).revokeUserSessions(any());
        verify(messagingTemplate).convertAndSendToUser(
                eq(chatOnlyUser.getUsername()),
                eq("/queue/chat"),
                argThat((Object evt) -> evt instanceof ChatWsEventDto ws && "CHAT_FROZEN".equals(ws.eventType()))
        );

        ArgumentCaptor<ChatSecurityEventEntity> secCaptor = ArgumentCaptor.forClass(ChatSecurityEventEntity.class);
        verify(securityEventRepo).save(secCaptor.capture());
        assertEquals("CHAT_USER_FROZEN", secCaptor.getValue().getEventType());
        assertEquals("HIGH", secCaptor.getValue().getSeverity());

        ArgumentCaptor<AuditLogEntity> auditCaptor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(auditLogRepositoryPort).log(auditCaptor.capture());
        assertFalse(auditCaptor.getValue().getDetails().isEmpty());

        GovernanceActionResponse unfreezeRes = governanceService.unfreezeUser(chatOnlyUser.getId());
        assertTrue(unfreezeRes.success());
        assertNull(chatOnlyUser.getChatFrozenUntil());
        assertNull(chatOnlyUser.getChatFrozenReason());
        assertTrue(chatOnlyUser.getIsEnabled());
        assertEquals(UserStatus.ACTIVE, chatOnlyUser.getStatus());
    }

    @Test
    void freezeAndUnfreeze_fullAccountSuspension_suspendsWmsRevokesTokensAndReactivatesOnUnfreeze() {
        when(userRepo.findByUsername(directorUser.getUsername())).thenReturn(Optional.of(directorUser));
        when(userRepo.findById(managerUser.getId())).thenReturn(Optional.of(managerUser));
        when(userRepo.save(any(UserEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(securityEventRepo.save(any(ChatSecurityEventEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(auditLogRepositoryPort.log(any(AuditLogEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        authenticateAs(directorUser.getUsername());

        GovernanceActionResponse freezeRes = governanceService.freezeUser(
                managerUser.getId(),
                new FreezeUserRequest("Exfiltración detectada", false, null)
        );

        assertTrue(freezeRes.success());
        assertNotNull(managerUser.getChatFrozenUntil());
        assertTrue(managerUser.getChatFrozenUntil().isAfter(Instant.now().plus(360, ChronoUnit.DAYS)));
        assertFalse(managerUser.getIsEnabled());
        assertEquals(UserStatus.SUSPENDED, managerUser.getStatus());

        verify(tokenBlacklistPort, times(1)).revokeUserSessions(managerUser.getId());
        verify(messagingTemplate).convertAndSendToUser(
                eq(managerUser.getUsername()),
                eq("/queue/chat"),
                argThat((Object evt) -> evt instanceof ChatWsEventDto ws && "CHAT_FROZEN".equals(ws.eventType()))
        );

        ArgumentCaptor<ChatSecurityEventEntity> secCaptor = ArgumentCaptor.forClass(ChatSecurityEventEntity.class);
        verify(securityEventRepo).save(secCaptor.capture());
        assertEquals("ACCOUNT_SUSPENDED", secCaptor.getValue().getEventType());
        assertEquals("CRITICAL", secCaptor.getValue().getSeverity());

        GovernanceActionResponse unfreezeRes = governanceService.unfreezeUser(managerUser.getId());
        assertTrue(unfreezeRes.success());
        assertNull(managerUser.getChatFrozenUntil());
        assertNull(managerUser.getChatFrozenReason());
        assertTrue(managerUser.getIsEnabled());
        assertEquals(UserStatus.ACTIVE, managerUser.getStatus());
    }

    @Test
    void chatService_blocksSendEditDeleteWhenSpaceFrozenOrArchived_andWhenUserChatFrozen() {
        UUID convId = UUID.randomUUID();
        UUID msgId = UUID.randomUUID();

        ChatConversationEntity frozenConv = ChatConversationEntity.builder()
                .id(convId)
                .organizationId(orgId)
                .type("SPACE")
                .name("Operaciones")
                .status("FROZEN")
                .build();

        ChatMessageEntity existingMsg = ChatMessageEntity.builder()
                .id(msgId)
                .conversationId(convId)
                .senderId(chatOnlyUser.getId())
                .content("Texto original")
                .createdAt(Instant.now())
                .build();

        authenticateAs(chatOnlyUser.getUsername());
        when(userRepo.findByUsername(chatOnlyUser.getUsername())).thenReturn(Optional.of(chatOnlyUser));
        when(conversationRepo.findById(convId)).thenReturn(Optional.of(frozenConv));
        when(participantRepo.existsByConversationIdAndUserIdAndIsActiveTrue(convId, chatOnlyUser.getId())).thenReturn(true);
        when(messageRepo.findById(msgId)).thenReturn(Optional.of(existingMsg));

        ResponseStatusException sendEx = assertThrows(ResponseStatusException.class, () ->
                chatService.sendMessage(convId, new SendMessageRequest("hola", null, "TEXT", null, null))
        );
        assertEquals(HttpStatus.FORBIDDEN, sendEx.getStatusCode());

        ResponseStatusException editEx = assertThrows(ResponseStatusException.class, () ->
                chatService.editMessage(msgId, new EditMessageRequest("editado"))
        );
        assertEquals(HttpStatus.FORBIDDEN, editEx.getStatusCode());

        ResponseStatusException deleteEx = assertThrows(ResponseStatusException.class, () ->
                chatService.deleteMessage(msgId)
        );
        assertEquals(HttpStatus.FORBIDDEN, deleteEx.getStatusCode());
        verify(messageRepo, never()).save(any());

        // Active space, but user has chatFrozenUntil in the future
        frozenConv.setStatus("ACTIVE");
        chatOnlyUser.setChatFrozenUntil(Instant.now().plus(2, ChronoUnit.HOURS));
        chatOnlyUser.setChatFrozenReason("Sanción temporal");

        ResponseStatusException userFrozenEx = assertThrows(ResponseStatusException.class, () ->
                chatService.sendMessage(convId, new SendMessageRequest("intento", null, "TEXT", null, null))
        );
        assertEquals(HttpStatus.FORBIDDEN, userFrozenEx.getStatusCode());
        assertTrue(userFrozenEx.getReason() != null && userFrozenEx.getReason().contains("Sanción temporal"));
    }

    @Test
    void chatService_editAndDelete_preserveOriginalContentSetDeletedByAndAuditExplicitly() {
        UUID convId = UUID.randomUUID();
        UUID msgId = UUID.randomUUID();

        ChatConversationEntity activeConv = ChatConversationEntity.builder()
                .id(convId)
                .organizationId(orgId)
                .type("SPACE")
                .name("General")
                .status("ACTIVE")
                .build();

        ChatMessageEntity msg = ChatMessageEntity.builder()
                .id(msgId)
                .conversationId(convId)
                .senderId(chatOnlyUser.getId())
                .content("Contenido inicial v1")
                .messageType("TEXT")
                .createdAt(Instant.now())
                .build();

        authenticateAs(chatOnlyUser.getUsername());
        when(userRepo.findByUsername(chatOnlyUser.getUsername())).thenReturn(Optional.of(chatOnlyUser));
        when(conversationRepo.findById(convId)).thenReturn(Optional.of(activeConv));
        when(participantRepo.existsByConversationIdAndUserIdAndIsActiveTrue(convId, chatOnlyUser.getId())).thenReturn(true);
        when(messageRepo.findById(msgId)).thenReturn(Optional.of(msg));
        when(messageRepo.save(any(ChatMessageEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reactionRepo.findByMessageIdIn(anyList())).thenReturn(List.of());
        when(attachmentRepo.findByMessageIdIn(anyList())).thenReturn(List.of());
        when(userRepo.findAllById(any())).thenReturn(List.of(chatOnlyUser));
        when(participantRepo.findByConversationIdAndIsActiveTrue(convId)).thenReturn(List.of());
        when(auditLogRepositoryPort.log(any(AuditLogEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        // 1st edit saves originalContent = "Contenido inicial v1"
        chatService.editMessage(msgId, new EditMessageRequest("Contenido editado v2"));
        assertEquals("Contenido inicial v1", msg.getOriginalContent());
        assertEquals("Contenido editado v2", msg.getContent());
        assertTrue(msg.getIsEdited());

        // 2nd edit keeps originalContent = "Contenido inicial v1"
        chatService.editMessage(msgId, new EditMessageRequest("Contenido editado v3"));
        assertEquals("Contenido inicial v1", msg.getOriginalContent());
        assertEquals("Contenido editado v3", msg.getContent());

        // Delete keeps originalContent, sets deletedBy, and logs explicit audit details
        chatService.deleteMessage(msgId);
        assertEquals("Contenido inicial v1", msg.getOriginalContent());
        assertEquals("Mensaje eliminado", msg.getContent());
        assertTrue(msg.getIsDeleted());
        assertEquals(chatOnlyUser.getId(), msg.getDeletedBy());
        assertNotNull(msg.getDeletedAt());

        ArgumentCaptor<AuditLogEntity> auditCaptor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(auditLogRepositoryPort, times(3)).log(auditCaptor.capture());
        List<AuditLogEntity> logs = auditCaptor.getAllValues();
        assertEquals("CHAT_MESSAGE_EDITED", logs.get(0).getAction());
        assertFalse(logs.get(0).getDetails().isEmpty());
        assertEquals("CHAT_MESSAGE_DELETED", logs.get(2).getAction());
        assertTrue(logs.get(2).getDetails().stream().anyMatch(d -> "deletedBy".equals(d.getFieldName())));
        assertTrue(logs.get(2).getDetails().stream().anyMatch(d ->
                "content".equals(d.getFieldName()) && "Contenido editado v3".equals(d.getOldValue())));
    }

    @Test
    void breakGlassInspect_validatesMandatoryReasonAndLogsAuditAndSecurityEventWithoutBase64() {
        UUID convId = UUID.randomUUID();
        when(userRepo.findByUsername(ceoUser.getUsername())).thenReturn(Optional.of(ceoUser));

        // Blank or < 10 chars rejected with 400
        ResponseStatusException shortReasonEx = assertThrows(ResponseStatusException.class, () ->
                governanceService.breakGlassInspect(convId, new BreakGlassRequest("corto"))
        );
        assertEquals(HttpStatus.BAD_REQUEST, shortReasonEx.getStatusCode());

        ResponseStatusException blankReasonEx = assertThrows(ResponseStatusException.class, () ->
                governanceService.breakGlassInspect(convId, new BreakGlassRequest("   "))
        );
        assertEquals(HttpStatus.BAD_REQUEST, blankReasonEx.getStatusCode());
        verify(auditLogRepositoryPort, never()).log(any());
        verify(securityEventRepo, never()).save(any());

        // Valid reason >= 10 chars succeeds
        ChatConversationEntity dm = ChatConversationEntity.builder()
                .id(convId)
                .organizationId(orgId)
                .type("DM")
                .build();
        UUID msgId = UUID.randomUUID();
        ChatMessageEntity deletedMsg = ChatMessageEntity.builder()
                .id(msgId)
                .conversationId(convId)
                .senderId(chatOnlyUser.getId())
                .content("Mensaje eliminado")
                .originalContent("Secreto filtrado")
                .isDeleted(true)
                .createdAt(Instant.now())
                .build();

        when(conversationRepo.findById(convId)).thenReturn(Optional.of(dm));
        when(messageRepo.findByConversationIdOrderByCreatedAtDesc(convId, PageRequest.of(0, 50)))
                .thenReturn(List.of(deletedMsg));
        when(attachmentRepo.countByMessageIdIn(List.of(msgId)))
                .thenReturn(List.<Object[]>of(new Object[]{msgId, 2L}));
        when(participantRepo.findByConversationIdInAndIsActiveTrue(List.of(convId)))
                .thenReturn(List.of(ChatParticipantEntity.builder().conversationId(convId).userId(chatOnlyUser.getId()).isActive(true).build()));
        when(userRepo.findAllById(any())).thenReturn(List.of(chatOnlyUser));
        when(auditLogRepositoryPort.log(any(AuditLogEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(securityEventRepo.save(any(ChatSecurityEventEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        BreakGlassTranscriptDto transcript = governanceService.breakGlassInspect(
                convId,
                new BreakGlassRequest("Investigación formal de fuga de información Folio-992")
        );

        assertEquals(convId, transcript.conversationId());
        assertEquals(1, transcript.messages().size());
        assertEquals("Secreto filtrado", transcript.messages().get(0).originalContent());
        assertEquals(2L, transcript.messages().get(0).attachmentCount());

        verify(attachmentRepo, never()).findByMessageIdIn(anyList());
        verify(attachmentRepo, never()).findById(any());

        ArgumentCaptor<AuditLogEntity> auditCaptor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(auditLogRepositoryPort).log(auditCaptor.capture());
        assertEquals("BREAK_GLASS_ACCESS", auditCaptor.getValue().getAction());
        assertFalse(auditCaptor.getValue().getDetails().isEmpty());

        ArgumentCaptor<ChatSecurityEventEntity> secCaptor = ArgumentCaptor.forClass(ChatSecurityEventEntity.class);
        verify(securityEventRepo).save(secCaptor.capture());
        assertEquals("BREAK_GLASS_ACCESS", secCaptor.getValue().getEventType());
        assertEquals("HIGH", secCaptor.getValue().getSeverity());
    }

    @Test
    void nonDirectorRoles_rejectedWith403AcrossAllGovernanceEndpoints() {
        authenticateAs(managerUser.getUsername());
        when(userRepo.findByUsername(managerUser.getUsername())).thenReturn(Optional.of(managerUser));

        UUID anyId = UUID.randomUUID();
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class, () -> governanceService.getOverview()).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class, () -> governanceService.listSpaces(null, null, 0, 20)).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class, () -> governanceService.updateSpaceStatus(anyId, new UpdateSpaceStatusRequest("FROZEN"))).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class, () -> governanceService.listUsers("ALL", null, 0, 20)).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class, () -> governanceService.freezeUser(anyId, new FreezeUserRequest("motivo", true, 12))).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class, () -> governanceService.unfreezeUser(anyId)).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class, () -> governanceService.getAuditMessages("ALL", 7, 0, 20)).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class, () -> governanceService.getAuditAttachments(7, 0, 20)).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class, () -> governanceService.downloadAuditAttachment(anyId)).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class, () -> governanceService.getDlpEvents(7, 0, 20)).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class, () -> governanceService.breakGlassInspect(anyId, new BreakGlassRequest("Motivo válido largo"))).getStatusCode());

        authenticateAs(chatOnlyUser.getUsername());
        when(userRepo.findByUsername(chatOnlyUser.getUsername())).thenReturn(Optional.of(chatOnlyUser));
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class, () -> governanceService.getOverview()).getStatusCode());
    }

    @Test
    void dlpReport_allowsAnyAuthenticatedChatUserAndExtractsIpAndUserAgent() {
        UUID convId = UUID.randomUUID();
        ChatConversationEntity conv = ChatConversationEntity.builder()
                .id(convId).organizationId(orgId).type("SPACE").name("Patio").build();

        authenticateAs(chatOnlyUser.getUsername());
        when(userRepo.findByUsername(chatOnlyUser.getUsername())).thenReturn(Optional.of(chatOnlyUser));
        when(conversationRepo.findById(convId)).thenReturn(Optional.of(conv));
        when(securityEventRepo.save(any(ChatSecurityEventEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "190.10.20.30, 10.0.0.1");
        request.addHeader("User-Agent", "4Guard-Desktop/1.0");

        DlpReportResponse res = governanceService.reportDlpEvent(
                new ReportDlpEventRequest(convId, "screenshot_attempt", "PrintScreen detectado"),
                request
        );

        assertTrue(res.recorded());
        ArgumentCaptor<ChatSecurityEventEntity> captor = ArgumentCaptor.forClass(ChatSecurityEventEntity.class);
        verify(securityEventRepo).save(captor.capture());
        assertEquals("SCREENSHOT_ATTEMPT", captor.getValue().getEventType());
        assertEquals("HIGH", captor.getValue().getSeverity());
        assertEquals("190.10.20.30", captor.getValue().getIpAddress());
        assertEquals("4Guard-Desktop/1.0", captor.getValue().getUserAgent());
    }

    @Test
    void auditAttachments_clampsPageSizeTo50AndExcludesStorageUrl_andDownloadAuditsAndChecksOrg() {
        when(userRepo.findByUsername(ceoUser.getUsername())).thenReturn(Optional.of(ceoUser));
        UUID attId = UUID.randomUUID();
        UUID msgId = UUID.randomUUID();
        UUID convId = UUID.randomUUID();

        AttachmentMetadataRow row = new AttachmentMetadataRow(
                attId, msgId, convId, "Operaciones", "SPACE", chatOnlyUser.getId(),
                "remision.pdf", "application/pdf", 1024L, Instant.now()
        );
        when(attachmentRepo.findMetadataByOrganizationIdAndCreatedAtAfter(eq(orgId), any(Instant.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(row), PageRequest.of(0, 50), 1));
        when(userRepo.findAllById(any())).thenReturn(List.of(chatOnlyUser));

        PagedResponse<GovernanceAttachmentDto> page = governanceService.getAuditAttachments(7, 0, 500);
        assertEquals(50, page.size());
        assertEquals(1, page.content().size());
        assertEquals("remision.pdf", page.content().get(0).fileName());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(attachmentRepo).findMetadataByOrganizationIdAndCreatedAtAfter(eq(orgId), any(), pageableCaptor.capture());
        assertEquals(50, pageableCaptor.getValue().getPageSize());

        // Download from another org must fail with 403
        ChatAttachmentEntity otherAtt = ChatAttachmentEntity.builder()
                .id(attId).messageId(msgId).fileName("secret.pdf").contentType("application/pdf").fileSize(100L).storageUrl("data:application/pdf;base64,AA==").build();
        ChatMessageEntity otherMsg = ChatMessageEntity.builder().id(msgId).conversationId(convId).senderId(UUID.randomUUID()).content("x").build();
        ChatConversationEntity otherConv = ChatConversationEntity.builder().id(convId).organizationId(otherOrgId).type("SPACE").build();

        when(attachmentRepo.findById(attId)).thenReturn(Optional.of(otherAtt));
        when(messageRepo.findById(msgId)).thenReturn(Optional.of(otherMsg));
        when(conversationRepo.findById(convId)).thenReturn(Optional.of(otherConv));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                governanceService.downloadAuditAttachment(attId)
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(auditLogRepositoryPort, never()).log(any());
    }

    @Test
    void sessionHardening_refreshTokenAndUserDetailsRejectSuspendedOrRevokedUsers() {
        UserEntity suspendedUser = buildUser(UUID.randomUUID(), "susp.user", "Susp", "User", org, "CHAT_ONLY");
        suspendedUser.setStatus(UserStatus.SUSPENDED);
        suspendedUser.setIsEnabled(false);

        when(userRepositoryPort.findByUsernameOrEmail("susp.user")).thenReturn(Optional.of(suspendedUser));
        assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername("susp.user"));

        // Refresh token rejected when user is suspended/disabled
        when(jwtService.extractUsername("rt-1")).thenReturn("susp.user");
        when(userRepositoryPort.findByUsername("susp.user")).thenReturn(Optional.of(suspendedUser));
        assertThrows(InvalidCredentialsException.class, () ->
                refreshTokenUseCase.refresh(RefreshTokenRequest.builder().refreshToken("rt-1").build())
        );

        // Refresh token rejected when session was revoked in TokenBlacklistPort
        Date issuedAt = new Date();
        when(jwtService.extractUsername("rt-2")).thenReturn(chatOnlyUser.getUsername());
        when(userRepositoryPort.findByUsername(chatOnlyUser.getUsername())).thenReturn(Optional.of(chatOnlyUser));
        when(jwtService.extractIssuedAt("rt-2")).thenReturn(issuedAt);
        when(tokenBlacklistPort.isUserRevoked(chatOnlyUser.getId(), issuedAt)).thenReturn(true);

        assertThrows(InvalidCredentialsException.class, () ->
                refreshTokenUseCase.refresh(RefreshTokenRequest.builder().refreshToken("rt-2").build())
        );
        verify(jwtService, never()).generateAccessToken(any());
    }

    @Test
    void jsonSerialization_preservesIsPrefixOnBooleanRecordFields() throws Exception {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        GovernanceChatUserDto userDto = new GovernanceChatUserDto(
                UUID.randomUUID(), "chat.sofia", "Sofia Ruiz", "sofia@4guard.local",
                "CHAT_ONLY", true, true, "ACTIVE", true, Instant.now(), null, null, 2L
        );
        String json = mapper.writeValueAsString(userDto);
        assertTrue(json.contains("\"isChatOnly\":true"));
        assertTrue(json.contains("\"isEnabled\":true"));
    }
}
