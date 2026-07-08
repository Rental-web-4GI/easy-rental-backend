package com.yowyob.easyrental.modules.support.infrastructure.adapter.in.web;

import com.yowyob.easyrental.modules.support.domain.port.in.SupportUseCase;
import com.yowyob.easyrental.modules.support.dto.SupportConfigResponseDTO;
import com.yowyob.easyrental.modules.support.dto.SupportConversationResponseDTO;
import com.yowyob.easyrental.modules.support.dto.SupportMessageRequest;
import com.yowyob.easyrental.modules.support.dto.SupportMessageResponseDTO;
import com.yowyob.easyrental.modules.support.dto.SupportReplyRequest;
import com.yowyob.easyrental.modules.support.dto.SupportSendResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
@Tag(name = "Support Messaging", description = "Messagerie support Easy Rental")
@SecurityRequirement(name = "bearerAuth")
public class SupportController {

    private final SupportUseCase supportUseCase;

    @Operation(summary = "Configuration publique du support")
    @GetMapping("/config")
    public Mono<ResponseEntity<SupportConfigResponseDTO>> getConfig() {
        return supportUseCase.getPublicConfig().map(ResponseEntity::ok);
    }

    @Operation(summary = "Envoyer un message utilisateur")
    @PostMapping("/messages")
    public Mono<ResponseEntity<SupportSendResponseDTO>> sendMessage(@RequestBody SupportMessageRequest request) {
        return supportUseCase.sendUserMessage(request).map(ResponseEntity::ok);
    }

    @Operation(summary = "Historique fusionné d'une conversation")
    @GetMapping("/conversation/messages")
    public Flux<SupportMessageResponseDTO> getConversationMessages(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String visitorSessionId) {
        return supportUseCase.getConversationMessages(email, visitorSessionId);
    }

    @Operation(summary = "Lister les conversations (Admin)")
    @GetMapping("/admin/conversations")
    @PreAuthorize("hasRole('ADMIN')")
    public Flux<SupportConversationResponseDTO> listConversations() {
        return supportUseCase.listConversationsForAdmin();
    }

    @Operation(summary = "Messages fusionnés d'une conversation (Admin)")
    @GetMapping("/admin/conversation/messages")
    @PreAuthorize("hasRole('ADMIN')")
    public Flux<SupportMessageResponseDTO> getAdminConversationMessages(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String visitorSessionId) {
        return supportUseCase.getConversationMessagesForAdmin(email, visitorSessionId);
    }

    @Operation(summary = "Répondre à une conversation (Admin)")
    @PostMapping("/admin/conversation/reply")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<SupportMessageResponseDTO>> reply(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String visitorSessionId,
            @RequestBody SupportReplyRequest request) {
        return supportUseCase.replyAsAdmin(email, visitorSessionId, request)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Marquer une conversation comme lue (Admin)")
    @PatchMapping("/admin/conversation/read")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<Void>> markConversationAsRead(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String visitorSessionId) {
        return supportUseCase.markConversationAsRead(email, visitorSessionId)
                .thenReturn(ResponseEntity.noContent().build());
    }
}
