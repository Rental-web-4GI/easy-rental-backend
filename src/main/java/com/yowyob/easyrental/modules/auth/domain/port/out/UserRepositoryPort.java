package com.yowyob.easyrental.modules.auth.domain.port.out;

import com.yowyob.easyrental.modules.auth.domain.UserEntity;
import java.util.UUID;
import reactor.core.publisher.Mono;

/**
 * Outgoing port for user persistence.
 */
public interface UserRepositoryPort {

    Mono<UserEntity> findByEmail(String email);

    Mono<UserEntity> findById(UUID id);

    Mono<UserEntity> save(UserEntity user);

    Mono<Void> clearUserReferences();
}
