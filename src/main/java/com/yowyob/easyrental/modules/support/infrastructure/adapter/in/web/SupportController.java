package com.yowyob.easyrental.modules.support.infrastructure.adapter.in.web;

import com.yowyob.easyrental.modules.support.domain.port.in.SupportUseCase;
import com.yowyob.easyrental.modules.support.dto.SupportConfigResponseDTO;
import com.yowyob.easyrental.modules.support.dto.SupportMessageRequest;
import com.yowyob.easyrental.modules.support.dto.SupportMessageResponseDTO;
import com.yowyob.easyrental.modules.support.dto.SupportReplyRequest;
import com.yowyob.easyrental.modules.support.dto.SupportSendResponseDTO;
import com.yowyob.easyrental.modules.support.dto.SupportThreadResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
@Tag(name = "Support Messaging", description = "Messagerie support Campus France")
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
    public Mono<ResponseEntity<SupportSendResponseDTO>> sendMessage(
            @Valid @RequestBody SupportMessageRequest request) {
        return supportUseCase.sendUserMessage(request).map(ResponseEntity::ok);
    }

    @Operation(summary = "Historique d'un fil de discussion")
    @GetMapping("/threads/{threadId}/messages")
    public Flux<SupportMessageResponseDTO> getMessages(@PathVariable UUID threadId) {
        return supportUseCase.getThreadMessages(threadId);
    }

    @Operation(summary = "Lister tous les fils (Admin)")
    @GetMapping("/threads")
    @PreAuthorize("hasRole('ADMIN')")
    public Flux<SupportThreadResponseDTO> listThreads() {
        return supportUseCase.listAllThreads();
    }

    @Operation(summary = "Répondre à un fil (Admin)")
    @PostMapping("/threads/{threadId}/reply")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<SupportMessageResponseDTO>> reply(
            @PathVariable UUID threadId,
            @Valid @RequestBody SupportReplyRequest request) {
        return supportUseCase.replyAsAdmin(threadId, request).map(ResponseEntity::ok);
    }
}
