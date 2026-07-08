package com.yowyob.easyrental.modules.support.domain.port.out;

import com.yowyob.easyrental.modules.support.domain.SupportMessageEntity;
import com.yowyob.easyrental.modules.support.domain.SupportThreadEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface SupportRepositoryPort {
    Mono<SupportThreadEntity> findThreadById(UUID id);
    Flux<SupportThreadEntity> findAllThreads();
    Mono<SupportThreadEntity> saveThread(SupportThreadEntity thread);
    Flux<SupportMessageEntity> findMessagesByThreadId(UUID threadId);
    Mono<SupportMessageEntity> saveMessage(SupportMessageEntity message);
}
