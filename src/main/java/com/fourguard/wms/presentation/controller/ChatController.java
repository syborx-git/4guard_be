package com.fourguard.wms.presentation.controller;

import com.fourguard.wms.application.dto.chat.ChatDtos.*;
import com.fourguard.wms.application.usecase.ChatService;
import com.fourguard.wms.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<ChatUserDto>>> listUsers(
            @RequestParam(required = false) UUID branchId
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Usuarios obtenidos", chatService.listOrganizationUsers(branchId)));
    }

    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<List<ConversationSummaryDto>>> listConversations() {
        return ResponseEntity.ok(ApiResponse.ok("Conversaciones obtenidas", chatService.listMyConversations()));
    }

    @PostMapping("/conversations/dm")
    public ResponseEntity<ApiResponse<ConversationSummaryDto>> getOrCreateDm(
            @Valid @RequestBody CreateDmRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("DM obtenido", chatService.getOrCreateDm(request)));
    }

    @PostMapping("/conversations/space")
    public ResponseEntity<ApiResponse<ConversationSummaryDto>> createSpace(
            @Valid @RequestBody CreateSpaceRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Espacio creado", chatService.createSpace(request)));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<ApiResponse<List<MessageDto>>> getMessages(
            @PathVariable UUID conversationId,
            @RequestParam(required = false) Instant before,
            @RequestParam(defaultValue = "40") int limit
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Mensajes obtenidos", chatService.getMessages(conversationId, before, limit)));
    }

    @PostMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<ApiResponse<MessageDto>> sendMessage(
            @PathVariable UUID conversationId,
            @Valid @RequestBody SendMessageRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Mensaje enviado", chatService.sendMessage(conversationId, request)));
    }

    @PutMapping("/messages/{messageId}")
    public ResponseEntity<ApiResponse<MessageDto>> editMessage(
            @PathVariable UUID messageId,
            @Valid @RequestBody EditMessageRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Mensaje editado", chatService.editMessage(messageId, request)));
    }

    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<ApiResponse<MessageDto>> deleteMessage(
            @PathVariable UUID messageId
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Mensaje eliminado", chatService.deleteMessage(messageId)));
    }

    @PostMapping("/messages/{messageId}/reactions")
    public ResponseEntity<ApiResponse<MessageDto>> toggleReaction(
            @PathVariable UUID messageId,
            @Valid @RequestBody ToggleReactionRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Reacción actualizada", chatService.toggleReaction(messageId, request)));
    }

    @PostMapping("/conversations/{conversationId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable UUID conversationId
    ) {
        chatService.markAsRead(conversationId);
        return ResponseEntity.ok(ApiResponse.ok("Conversación marcada como leída"));
    }

    @PutMapping("/conversations/{conversationId}/notifications")
    public ResponseEntity<ApiResponse<Void>> updateNotificationPref(
            @PathVariable UUID conversationId,
            @Valid @RequestBody UpdateNotificationPrefRequest request
    ) {
        chatService.updateNotificationPref(conversationId, request);
        return ResponseEntity.ok(ApiResponse.ok("Preferencia actualizada"));
    }

    @PostMapping("/conversations/{conversationId}/typing")
    public ResponseEntity<ApiResponse<Void>> typing(
            @PathVariable UUID conversationId
    ) {
        chatService.broadcastTyping(conversationId);
        return ResponseEntity.ok(ApiResponse.ok("Typing enviado"));
    }
}
