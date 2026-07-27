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
import com.yowyob.easyrental.modules.notification.domain.NotificationTemplate;
import com.yowyob.easyrental.modules.notification.domain.port.in.NotificationUseCase;
import com.yowyob.easyrental.shared.enums.NotificationReason;
import com.yowyob.easyrental.shared.enums.NotificationResourceType;
import com.yowyob.easyrental.shared.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class ConversationUseCaseImpl implements ConversationUseCase {

    private final ConversationRepositoryPort conversationRepository;
    private final ConversationMapper mapper;
    private final NotificationUseCase notificationUseCase;

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
                    if (!isParticipant(conversation, senderType, senderId)) {
                        return Mono.<ConversationMessageEntity>error(new ValidationException("NOT_A_PARTICIPANT"));
                    }
                    boolean senderIsA = isParticipantA(conversation, senderType, senderId);
                    Instant now = Instant.now();
                    if (senderIsA) {
                        conversation.setBUnread(nz(conversation.getBUnread()) + 1);
                    } else {
                        conversation.setAUnread(nz(conversation.getAUnread()) + 1);
                    }
                    conversation.setLastMessageAt(now);

                    ParticipantType recipientType = senderIsA
                            ? conversation.getParticipantBType() : conversation.getParticipantAType();
                    UUID recipientId = senderIsA
                            ? conversation.getParticipantBId() : conversation.getParticipantAId();

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
                            .then(conversationRepository.saveMessage(message))
                            .flatMap(saved -> notifyRecipient(conversationId, recipientType, recipientId)
                                    .thenReturn(saved));
                })
                .map(mapper::toMessageDto);
    }

    /**
     * Notifies the recipient of a new message. Best-effort: any failure is
     * swallowed so it never breaks message sending. Admin recipients are
     * skipped (no per-user notification inbox for the support pole here).
     */
    private Mono<Void> notifyRecipient(UUID conversationId, ParticipantType recipientType, UUID recipientId) {
        if (recipientType == null || recipientId == null || recipientType == ParticipantType.ADMIN) {
            return Mono.empty();
        }
        NotificationResourceType resourceType = recipientType == ParticipantType.CLIENT
                ? NotificationResourceType.CLIENT : NotificationResourceType.AGENCY;

        return notificationUseCase.createNotification(
                        conversationId, recipientId, resourceType,
                        NotificationReason.MESSAGE, null, null,
                        NotificationTemplate.NEW_MESSAGE)
                .then()
                .onErrorResume(e -> {
                    log.warn("Failed to notify recipient {} of new message on conversation {}: {}",
                            recipientId, conversationId, e.getMessage());
                    return Mono.empty();
                });
    }

    @Override
    public Flux<ConversationDTO> listForParticipant(ParticipantType type, UUID id) {
        return conversationRepository.findForParticipant(type.name(), id)
                .map(c -> mapper.toDto(c, type, id));
    }

    @Override
    public Flux<MessageDTO> getMessages(UUID conversationId, ParticipantType callerType, UUID callerId,
                                        int page, int size) {
        // ADMIN a un droit de supervision (lecture seule) sur toute conversation ;
        // les autres appelants ne lisent que les conversations dont ils sont membres.
        return conversationRepository.findById(conversationId)
                .switchIfEmpty(Mono.error(new ValidationException("CONVERSATION_NOT_FOUND")))
                .filter(conversation -> callerType == ParticipantType.ADMIN
                        || isParticipant(conversation, callerType, callerId))
                .switchIfEmpty(Mono.error(new ValidationException("NOT_A_PARTICIPANT")))
                .flatMapMany(conversation -> conversationRepository.findMessages(conversationId, page, size))
                .map(mapper::toMessageDto);
    }

    @Override
    public Mono<Void> markRead(UUID conversationId, ParticipantType readerType, UUID readerId) {
        return conversationRepository.findById(conversationId)
                .switchIfEmpty(Mono.error(new ValidationException("CONVERSATION_NOT_FOUND")))
                .flatMap(conversation -> {
                    if (!isParticipant(conversation, readerType, readerId)) {
                        return Mono.<ConversationEntity>error(new ValidationException("NOT_A_PARTICIPANT"));
                    }
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

    private boolean isParticipantB(ConversationEntity conversation, ParticipantType type, UUID id) {
        return conversation.getParticipantBType() == type && Objects.equals(conversation.getParticipantBId(), id);
    }

    /** True when (type,id) is one of the two participants of the conversation. */
    private boolean isParticipant(ConversationEntity conversation, ParticipantType type, UUID id) {
        return isParticipantA(conversation, type, id) || isParticipantB(conversation, type, id);
    }

    private int nz(Integer value) {
        return value != null ? value : 0;
    }
}
