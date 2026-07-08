package com.yowyob.easyrental.modules.support.infrastructure.adapter.out.persistence;

import com.yowyob.easyrental.modules.support.domain.SupportMessageEntity;
import com.yowyob.easyrental.modules.support.domain.SupportThreadEntity;
import com.yowyob.easyrental.modules.support.domain.port.out.SupportRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SupportRepositoryAdapter implements SupportRepositoryPort {

    private final SupportThreadRepository threadRepository;
    private final SupportMessageRepository messageRepository;

    @Override
    public Mono<SupportThreadEntity> findThreadById(UUID id) {
        return threadRepository.findById(Objects.requireNonNull(id));
    }

    @Override
    public Mono<SupportThreadEntity> findLatestThreadByVisitorEmail(String visitorEmail) {
        return threadRepository.findLatestByVisitorEmail(visitorEmail.trim().toLowerCase());
    }

    @Override
    public Mono<SupportThreadEntity> findLatestThreadByVisitorSessionId(String visitorSessionId) {
        return threadRepository.findLatestByVisitorSessionId(visitorSessionId.trim());
    }

    @Override
    public Flux<SupportThreadEntity> findAllThreads() {
        return threadRepository.findAllOrdered();
    }

    @Override
    public Flux<SupportThreadEntity> findAllThreadsByVisitorEmail(String visitorEmail) {
        return threadRepository.findAllByVisitorEmail(visitorEmail.trim().toLowerCase());
    }

    @Override
    public Flux<SupportThreadEntity> findAllThreadsByVisitorSessionId(String visitorSessionId) {
        return threadRepository.findAllByVisitorSessionId(visitorSessionId.trim());
    }

    @Override
    public Mono<SupportThreadEntity> saveThread(SupportThreadEntity thread) {
        return threadRepository.save(Objects.requireNonNull(thread));
    }

    @Override
    public Flux<SupportMessageEntity> findMessagesByThreadId(UUID threadId) {
        return messageRepository.findByThreadIdOrderByCreatedAt(threadId);
    }

    @Override
    public Flux<SupportMessageEntity> findAllMessagesByVisitorEmail(String visitorEmail) {
        return messageRepository.findAllByVisitorEmailOrderByCreatedAt(visitorEmail.trim().toLowerCase());
    }

    @Override
    public Flux<SupportMessageEntity> findAllMessagesByVisitorSessionId(String visitorSessionId) {
        return messageRepository.findAllByVisitorSessionIdOrderByCreatedAt(visitorSessionId.trim());
    }

    @Override
    public Mono<SupportMessageEntity> saveMessage(SupportMessageEntity message) {
        return messageRepository.save(Objects.requireNonNull(message));
    }
}
