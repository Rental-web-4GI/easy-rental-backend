package com.yowyob.easyrental.modules.conversation.application;

import com.yowyob.easyrental.modules.conversation.domain.ConversationEntity;
import com.yowyob.easyrental.modules.conversation.domain.ConversationMessageEntity;
import com.yowyob.easyrental.modules.conversation.domain.ConversationType;
import com.yowyob.easyrental.modules.conversation.domain.ParticipantType;
import com.yowyob.easyrental.modules.conversation.domain.port.in.ConversationUseCase;
import com.yowyob.easyrental.modules.conversation.domain.port.out.ConversationRepositoryPort;
import com.yowyob.easyrental.modules.conversation.dto.ConversationDTO;
import com.yowyob.easyrental.modules.conversation.dto.MessageDTO;
import com.yowyob.easyrental.modules.conversation.mapper.ConversationMapper;
import com.yowyob.easyrental.shared.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Implements the 2-participant conversation use cases: thread
 * creation/lookup, messaging with unread tracking, listing and read receipts.
 *
 * @author Easy Rental Team
 * @since 2026-07-26
 */
@Service
@RequiredArgsConstructor
public class ConversationUseCaseImpl implements ConversationUseCase {

    private final ConversationRepositoryPort conversationRepository;
    private final ConversationMapper mapper;

    @Override
    public Mono<ConversationEntity> openOrGet(ParticipantType aType, UUID aId, ParticipantType bType, UUID bId) {
        return conversationRepository.findBetween(aType.name(), aId, bType.name(), bId)
                .switchIfEmpty(Mono.defer(() -> {
                    ConversationEntity toSave = ConversationEntity.builder()
                            .id(UUID.randomUUID())
                            .type(ConversationType.deduce(aType, bType))
                            .participantAType(aType)
                            .participantAId(aId)
                            .participantBType(bType)
                            .participantBId(bId)
                            .aUnread(0)
                            .bUnread(0)
                            .createdAt(Instant.now())
                            .isNewRecord(true)
                            .build();
                    return conversationRepository.save(toSave);
                }));
    }

    @Override
    public Mono<MessageDTO> sendMessage(UUID conversationId, ParticipantType senderType, UUID senderId, String body) {
        if (body == null || body.isBlank()) {
            return Mono.error(new ValidationException("EMPTY_MESSAGE"));
        }

        return conversationRepository.findById(conversationId)
                .switchIfEmpty(Mono.error(new ValidationException("CONVERSATION_NOT_FOUND")))
                .flatMap(conversation -> {
                    boolean senderIsA = isParticipantA(conversation, senderType, senderId);
                    Instant now = Instant.now();
                    if (senderIsA) {
                        conversation.setBUnread(nz(conversation.getBUnread()) + 1);
                    } else {
                        conversation.setAUnread(nz(conversation.getAUnread()) + 1);
                    }
                    conversation.setLastMessageAt(now);

                    ConversationMessageEntity message = ConversationMessageEntity.builder()
                            .id(UUID.randomUUID())
                            .conversationId(conversationId)
                            .senderType(senderType)
                            .senderId(senderId)
                            .body(body)
                            .createdAt(now)
                            .isNewRecord(true)
                            .build();

                    return conversationRepository.save(conversation)
                            .then(conversationRepository.saveMessage(message));
                })
                .map(mapper::toMessageDto);
    }

    @Override
    public Flux<ConversationDTO> listForParticipant(ParticipantType type, UUID id) {
        return conversationRepository.findForParticipant(type.name(), id)
                .map(c -> mapper.toDto(c, type, id));
    }

    @Override
    public Flux<MessageDTO> getMessages(UUID conversationId, int page, int size) {
        return conversationRepository.findMessages(conversationId, page, size)
                .map(mapper::toMessageDto);
    }

    @Override
    public Mono<Void> markRead(UUID conversationId, ParticipantType readerType, UUID readerId) {
        return conversationRepository.findById(conversationId)
                .switchIfEmpty(Mono.error(new ValidationException("CONVERSATION_NOT_FOUND")))
                .flatMap(conversation -> {
                    if (isParticipantA(conversation, readerType, readerId)) {
                        conversation.setAUnread(0);
                    } else {
                        conversation.setBUnread(0);
                    }
                    return conversationRepository.save(conversation);
                })
                .then();
    }

    @Override
    public Flux<ConversationDTO> adminListAll(int page, int size) {
        return conversationRepository.findAll(page, size)
                .map(c -> mapper.toDto(c, ParticipantType.ADMIN, null));
    }

    private boolean isParticipantA(ConversationEntity conversation, ParticipantType type, UUID id) {
        return conversation.getParticipantAType() == type && Objects.equals(conversation.getParticipantAId(), id);
    }

    private int nz(Integer value) {
        return value != null ? value : 0;
    }
}
