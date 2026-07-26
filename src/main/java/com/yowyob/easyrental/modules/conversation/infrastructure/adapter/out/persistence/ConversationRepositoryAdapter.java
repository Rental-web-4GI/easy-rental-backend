package com.yowyob.easyrental.modules.conversation.infrastructure.adapter.out.persistence;

import com.yowyob.easyrental.modules.conversation.domain.ConversationEntity;
import com.yowyob.easyrental.modules.conversation.domain.ConversationMessageEntity;
import com.yowyob.easyrental.modules.conversation.domain.port.out.ConversationRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Adapter implementing {@link ConversationRepositoryPort} via R2DBC.
 *
 * @author Easy Rental Team
 * @since 2026-07-26
 */
@Component
@RequiredArgsConstructor
public class ConversationRepositoryAdapter implements ConversationRepositoryPort {

    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository conversationMessageRepository;

    @Override
    public Mono<ConversationEntity> save(ConversationEntity e) {
        return conversationRepository.save(e);
    }

    @Override
    public Mono<ConversationEntity> findById(UUID id) {
        return conversationRepository.findById(id);
    }

    @Override
    public Mono<ConversationEntity> findBetween(String aType, UUID aId, String bType, UUID bId) {
        return conversationRepository.findBetween(aType, aId, bType, bId);
    }

    @Override
    public Flux<ConversationEntity> findForParticipant(String type, UUID id) {
        return conversationRepository.findForParticipant(type, id);
    }

    @Override
    public Flux<ConversationEntity> findAll(int page, int size) {
        return conversationRepository.findAllPaged((long) page * size, size);
    }

    @Override
    public Mono<ConversationMessageEntity> saveMessage(ConversationMessageEntity m) {
        return conversationMessageRepository.save(m);
    }

    @Override
    public Flux<ConversationMessageEntity> findMessages(UUID conversationId, int page, int size) {
        return conversationMessageRepository.findByConversationPaged(conversationId, (long) page * size, size);
    }
}
