package com.yowyob.easyrental.modules.conversation.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * R2DBC entity mapped on the {@code conversation_messages} table (migration 37 from R3).
 *
 * @author Easy Rental Team
 * @since 2026-07-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("conversation_messages")
public class ConversationMessageEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    private UUID conversationId;
    private ParticipantType senderType;
    private UUID senderId;
    private String body;
    private Instant createdAt;

    @Transient
    @Builder.Default
    private boolean isNewRecord = false;

    @Override
    public boolean isNew() {
        return isNewRecord || id == null;
    }
}
