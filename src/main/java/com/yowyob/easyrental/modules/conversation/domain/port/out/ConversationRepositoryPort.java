package com.yowyob.easyrental.modules.conversation.domain.port.out;

import com.yowyob.easyrental.modules.conversation.domain.ConversationEntity;
import com.yowyob.easyrental.modules.conversation.domain.ConversationMessageEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Outgoing port for conversation and message persistence.
 *
 * @author Easy Rental Team
 * @since 2026-07-26
 */
public interface ConversationRepositoryPort {

    Mono<ConversationEntity> save(ConversationEntity e);

    Mono<ConversationEntity> findById(UUID id);

    Mono<ConversationEntity> findBetween(String aType, UUID aId, String bType, UUID bId);

    Flux<ConversationEntity> findForParticipant(String type, UUID id);

    Flux<ConversationEntity> findAll(int page, int size);

    Mono<ConversationMessageEntity> saveMessage(ConversationMessageEntity m);

    Flux<ConversationMessageEntity> findMessages(UUID conversationId, int page, int size);
}
