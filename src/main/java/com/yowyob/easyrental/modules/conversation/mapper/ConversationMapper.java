package com.yowyob.easyrental.modules.conversation.mapper;

import com.yowyob.easyrental.modules.conversation.domain.ConversationEntity;
import com.yowyob.easyrental.modules.conversation.domain.ConversationMessageEntity;
import com.yowyob.easyrental.modules.conversation.domain.ParticipantType;
import com.yowyob.easyrental.modules.conversation.dto.ConversationDTO;
import com.yowyob.easyrental.modules.conversation.dto.MessageDTO;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Maps conversation domain entities to API response DTOs.
 *
 * @author Easy Rental Team
 * @since 2026-07-26
 */
@Component
public class ConversationMapper {

    public ConversationDTO toDto(ConversationEntity entity, ParticipantType viewerType, java.util.UUID viewerId,
                                 String participantAName, String participantBName) {
        boolean isA = entity.getParticipantAType() == viewerType
                && Objects.equals(entity.getParticipantAId(), viewerId);
        int unread = isA ? nz(entity.getAUnread()) : nz(entity.getBUnread());

        return new ConversationDTO(
                entity.getId(),
                entity.getType() != null ? entity.getType().name() : null,
                entity.getParticipantAType() != null ? entity.getParticipantAType().name() : null,
                entity.getParticipantAId(),
                participantAName,
                entity.getParticipantBType() != null ? entity.getParticipantBType().name() : null,
                entity.getParticipantBId(),
                participantBName,
                entity.getSubject(),
                unread,
                entity.getLastMessageAt()
        );
    }

    public MessageDTO toMessageDto(ConversationMessageEntity entity) {
        return new MessageDTO(
                entity.getId(),
                entity.getConversationId(),
                entity.getSenderType() != null ? entity.getSenderType().name() : null,
                entity.getSenderId(),
                entity.getBody(),
                entity.getCreatedAt()
        );
    }

    private int nz(Integer value) {
        return value != null ? value : 0;
    }
}
