package com.yowyob.easyrental.modules.conversation.domain.port.in;

import com.yowyob.easyrental.modules.conversation.domain.ConversationEntity;
import com.yowyob.easyrental.modules.conversation.domain.ParticipantType;
import com.yowyob.easyrental.modules.conversation.dto.ConversationDTO;
import com.yowyob.easyrental.modules.conversation.dto.MessageDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Inbound port for the 2-participant conversation use cases: thread
 * creation/lookup, messaging, listing and unread tracking.
 *
 * @author Easy Rental Team
 * @since 2026-07-26
 */
public interface ConversationUseCase {

    Mono<ConversationEntity> openOrGet(ParticipantType aType, UUID aId, ParticipantType bType, UUID bId);

    Mono<MessageDTO> sendMessage(UUID conversationId, ParticipantType senderType, UUID senderId, String body);

    Flux<ConversationDTO> listForParticipant(ParticipantType type, UUID id);

    Flux<MessageDTO> getMessages(UUID conversationId, ParticipantType callerType, UUID callerId, int page, int size);

    Mono<Void> markRead(UUID conversationId, ParticipantType readerType, UUID readerId);

    Flux<ConversationDTO> adminListAll(int page, int size);
}
