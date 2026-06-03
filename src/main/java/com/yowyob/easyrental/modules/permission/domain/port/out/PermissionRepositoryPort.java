package com.yowyob.easyrental.modules.permission.domain.port.out;

import com.yowyob.easyrental.modules.permission.domain.PermissionEntity;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Outgoing port for permission persistence.
 */
public interface PermissionRepositoryPort {

    Flux<PermissionEntity> findAll();

    Mono<PermissionEntity> findById(UUID id);

    Flux<PermissionEntity> findByPosteId(UUID posteId);
}
