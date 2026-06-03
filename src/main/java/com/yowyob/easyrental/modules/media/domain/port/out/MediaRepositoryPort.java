package com.yowyob.easyrental.modules.media.domain.port.out;

import com.yowyob.easyrental.modules.media.domain.MediaEntity;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Outgoing port for media persistence.
 */
public interface MediaRepositoryPort {
    Mono<MediaEntity> findById(UUID id);
    Mono<MediaEntity> save(MediaEntity entity);
    Flux<MediaEntity> findAll();
    Mono<Void> deleteById(UUID id);
}
