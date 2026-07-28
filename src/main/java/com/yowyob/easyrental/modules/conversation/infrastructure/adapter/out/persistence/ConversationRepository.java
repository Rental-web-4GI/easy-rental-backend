package com.yowyob.easyrental.modules.conversation.infrastructure.adapter.out.persistence;

import com.yowyob.easyrental.modules.conversation.domain.ConversationEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Spring Data R2DBC repository for {@link ConversationEntity}.
 *
 * @author Easy Rental Team
 * @since 2026-07-26
 */
@Repository
public interface ConversationRepository extends R2dbcRepository<ConversationEntity, UUID> {

    @Query("""
       SELECT * FROM conversations
       WHERE (participant_a_type = :aType AND participant_a_id IS NOT DISTINCT FROM :aId
              AND participant_b_type = :bType AND participant_b_id IS NOT DISTINCT FROM :bId)
          OR (participant_a_type = :bType AND participant_a_id IS NOT DISTINCT FROM :bId
              AND participant_b_type = :aType AND participant_b_id IS NOT DISTINCT FROM :aId)
       LIMIT 1
    """)
    Mono<ConversationEntity> findBetween(String aType, UUID aId, String bType, UUID bId);

    @Query("""
       SELECT * FROM conversations
       WHERE (participant_a_type = :type AND participant_a_id IS NOT DISTINCT FROM :id)
          OR (participant_b_type = :type AND participant_b_id IS NOT DISTINCT FROM :id)
       ORDER BY last_message_at DESC NULLS LAST
    """)
    Flux<ConversationEntity> findForParticipant(String type, UUID id);

    @Query("SELECT * FROM conversations ORDER BY last_message_at DESC NULLS LAST OFFSET :off LIMIT :lim")
    Flux<ConversationEntity> findAllPaged(long off, int lim);
}
