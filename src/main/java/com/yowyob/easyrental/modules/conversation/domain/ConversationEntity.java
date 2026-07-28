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
 * R2DBC entity mapped on the {@code conversations} table (migration 37 from R3).
 * A conversation always has exactly two participants (A and B).
 *
 * @author Easy Rental Team
 * @since 2026-07-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("conversations")
public class ConversationEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    private ConversationType type;
    private ParticipantType participantAType;
    private UUID participantAId;
    private ParticipantType participantBType;
    private UUID participantBId;
    private String subject;
    private Integer aUnread;
    private Integer bUnread;
    private Instant lastMessageAt;
    private Instant createdAt;

    @Transient
    @Builder.Default
    private boolean isNewRecord = false;

    @Override
    public boolean isNew() {
        return isNewRecord || id == null;
    }
}
