package com.yowyob.easyrental.modules.support.infrastructure.adapter.out.persistence;

import com.yowyob.easyrental.modules.support.domain.SupportMessageEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface SupportMessageRepository extends R2dbcRepository<SupportMessageEntity, UUID> {

    @Query("SELECT * FROM support_messages WHERE thread_id = :threadId ORDER BY created_at ASC")
    Flux<SupportMessageEntity> findByThreadIdOrderByCreatedAt(UUID threadId);
}
