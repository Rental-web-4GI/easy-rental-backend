package com.yowyob.easyrental.modules.media.infrastructure.adapter.out.persistence;

import com.yowyob.easyrental.modules.media.domain.MediaEntity;
import com.yowyob.easyrental.modules.media.domain.port.out.MediaRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MediaRepositoryAdapter implements MediaRepositoryPort {

    private final MediaRepository mediaRepository;

    @Override
    public Mono<MediaEntity> findById(UUID id) {
        return mediaRepository.findById(id);
    }

    @Override
    public Mono<MediaEntity> save(MediaEntity entity) {
        return mediaRepository.save(entity);
    }

    @Override
    public Flux<MediaEntity> findAll() {
        return mediaRepository.findAll();
    }

    @Override
    public Mono<Void> deleteById(UUID id) {
        return mediaRepository.deleteById(id);
    }
}
