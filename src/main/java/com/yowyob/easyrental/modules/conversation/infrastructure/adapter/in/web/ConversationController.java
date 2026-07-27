package com.yowyob.easyrental.modules.conversation.infrastructure.adapter.in.web;

import com.yowyob.easyrental.modules.auth.domain.UserEntity;
import com.yowyob.easyrental.modules.auth.domain.port.out.AuthUserPort;
import com.yowyob.easyrental.modules.conversation.domain.ConversationEntity;
import com.yowyob.easyrental.modules.conversation.domain.ParticipantType;
import com.yowyob.easyrental.modules.conversation.domain.port.in.ConversationUseCase;
import com.yowyob.easyrental.modules.conversation.dto.ConversationDTO;
import com.yowyob.easyrental.modules.conversation.dto.MessageDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * REST adapter for the 2-participant conversation module: opening/reusing
 * threads, sending/listing messages, marking read and admin oversight.
 *
 * @author Easy Rental Team
 * @since 2026-07-26
 */
@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
@Tag(name = "Conversations")
@SecurityRequirement(name = "bearerAuth")
public class ConversationController {

    private final ConversationUseCase useCase;
    private final AuthUserPort authUserPort;

    /** The caller resolved to their conversation participant identity. */
    private record ParticipantRef(ParticipantType type, UUID id) {
    }

    public record OpenRequest(String targetType, UUID targetId) {
    }

    public record SendRequest(String body) {
    }

    @Operation(summary = "Open (or reuse) a conversation with another participant")
    @PostMapping("/open")
    @PreAuthorize("isAuthenticated()")
    public Mono<ResponseEntity<ConversationEntity>> open(@RequestBody OpenRequest request) {
        return currentParticipant()
                .flatMap(caller -> useCase.openOrGet(
                        caller.type(), caller.id(),
                        ParticipantType.valueOf(request.targetType()), request.targetId()))
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Send a message in a conversation")
    @PostMapping("/{id}/messages")
    @PreAuthorize("isAuthenticated()")
    public Mono<ResponseEntity<MessageDTO>> sendMessage(@PathVariable UUID id, @RequestBody SendRequest request) {
        return currentParticipant()
                .flatMap(caller -> useCase.sendMessage(id, caller.type(), caller.id(), request.body()))
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "List messages of a conversation")
    @GetMapping("/{id}/messages")
    @PreAuthorize("isAuthenticated()")
    public Flux<MessageDTO> getMessages(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return currentParticipant()
                .flatMapMany(caller -> useCase.getMessages(id, caller.type(), caller.id(), page, size));
    }

    @Operation(summary = "List conversations for the current caller")
    @GetMapping("/mine")
    @PreAuthorize("isAuthenticated()")
    public Flux<ConversationDTO> mine() {
        return currentParticipant()
                .flatMapMany(caller -> useCase.listForParticipant(caller.type(), caller.id()));
    }

    @Operation(summary = "Mark a conversation as read for the current caller")
    @PutMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public Mono<ResponseEntity<Void>> markRead(@PathVariable UUID id) {
        return currentParticipant()
                .flatMap(caller -> useCase.markRead(id, caller.type(), caller.id()))
                .thenReturn(ResponseEntity.noContent().build());
    }

    @Operation(summary = "Admin: list all conversations")
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public Flux<ConversationDTO> adminAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return useCase.adminListAll(page, size);
    }

    private Mono<ParticipantRef> currentParticipant() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getName())
                .flatMap(authUserPort::findByEmail)
                .map(user -> {
                    ParticipantType type = roleToType(user.getRole());
                    return new ParticipantRef(type, idForType(type, user));
                });
    }

    private ParticipantType roleToType(String role) {
        if ("CLIENT".equalsIgnoreCase(role)) {
            return ParticipantType.CLIENT;
        }
        if ("ADMIN".equalsIgnoreCase(role)) {
            return ParticipantType.ADMIN;
        }
        return ParticipantType.AGENCY; // STAFF, ORGANIZATION
    }

    private UUID idForType(ParticipantType type, UserEntity u) {
        return switch (type) {
            case CLIENT -> u.getId();
            case AGENCY -> u.getAgencyId();
            case ADMIN -> null;
        };
    }
}
