package com.yowyob.easyrental.modules.support.infrastructure.adapter.out.persistence;

import com.yowyob.easyrental.modules.support.domain.SupportThreadEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface SupportThreadRepository extends R2dbcRepository<SupportThreadEntity, UUID> {

    @Query("SELECT * FROM support_threads ORDER BY last_message_at DESC NULLS LAST, created_at DESC")
    Flux<SupportThreadEntity> findAllOrdered();

    @Query("""
            SELECT * FROM support_threads
            WHERE LOWER(visitor_email) = LOWER(:visitorEmail)
            ORDER BY last_message_at DESC NULLS LAST, created_at DESC
            LIMIT 1
            """)
    Mono<SupportThreadEntity> findLatestByVisitorEmail(String visitorEmail);

    @Query("""
            SELECT * FROM support_threads
            WHERE visitor_session_id = :visitorSessionId
            ORDER BY last_message_at DESC NULLS LAST, created_at DESC
            LIMIT 1
            """)
    Mono<SupportThreadEntity> findLatestByVisitorSessionId(String visitorSessionId);

    @Query("""
            SELECT * FROM support_threads
            WHERE LOWER(visitor_email) = LOWER(:visitorEmail)
            ORDER BY last_message_at DESC NULLS LAST, created_at DESC
            """)
    Flux<SupportThreadEntity> findAllByVisitorEmail(String visitorEmail);

    @Query("""
            SELECT * FROM support_threads
            WHERE visitor_session_id = :visitorSessionId
            ORDER BY last_message_at DESC NULLS LAST, created_at DESC
            """)
    Flux<SupportThreadEntity> findAllByVisitorSessionId(String visitorSessionId);
}
