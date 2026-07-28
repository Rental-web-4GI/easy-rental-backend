package com.yowyob.easyrental.modules.conversation.infrastructure.adapter.out.persistence;

import com.yowyob.easyrental.modules.conversation.domain.ConversationMessageEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * Spring Data R2DBC repository for {@link ConversationMessageEntity}.
 *
 * @author Easy Rental Team
 * @since 2026-07-26
 */
@Repository
public interface ConversationMessageRepository extends R2dbcRepository<ConversationMessageEntity, UUID> {

    @Query("""
       SELECT * FROM conversation_messages
       WHERE conversation_id = :cid
       ORDER BY created_at ASC OFFSET :off LIMIT :lim
    """)
    Flux<ConversationMessageEntity> findByConversationPaged(UUID cid, long off, int lim);
}
