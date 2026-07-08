package com.yowyob.easyrental.modules.support.domain.port.out;

import com.yowyob.easyrental.modules.support.domain.SupportMessageEntity;
import com.yowyob.easyrental.modules.support.domain.SupportThreadEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface SupportRepositoryPort {

    Mono<SupportThreadEntity> findThreadById(UUID id);

    Mono<SupportThreadEntity> findLatestThreadByVisitorEmail(String visitorEmail);

    Mono<SupportThreadEntity> findLatestThreadByVisitorSessionId(String visitorSessionId);

    Flux<SupportThreadEntity> findAllThreads();

    Flux<SupportThreadEntity> findAllThreadsByVisitorEmail(String visitorEmail);

    Flux<SupportThreadEntity> findAllThreadsByVisitorSessionId(String visitorSessionId);

    Mono<SupportThreadEntity> saveThread(SupportThreadEntity thread);

    Flux<SupportMessageEntity> findMessagesByThreadId(UUID threadId);

    Flux<SupportMessageEntity> findAllMessagesByVisitorEmail(String visitorEmail);

    Flux<SupportMessageEntity> findAllMessagesByVisitorSessionId(String visitorSessionId);

    Mono<SupportMessageEntity> saveMessage(SupportMessageEntity message);
}
