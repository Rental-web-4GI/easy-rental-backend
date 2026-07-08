package com.yowyob.easyrental.modules.support.infrastructure.adapter.out.persistence;

import com.yowyob.easyrental.modules.support.domain.SupportThreadEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface SupportThreadRepository extends R2dbcRepository<SupportThreadEntity, UUID> {

    @Query("SELECT * FROM support_threads ORDER BY last_message_at DESC NULLS LAST, created_at DESC")
    Flux<SupportThreadEntity> findAllOrdered();
}
