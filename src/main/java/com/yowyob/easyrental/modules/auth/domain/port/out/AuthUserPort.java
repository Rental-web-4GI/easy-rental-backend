package com.yowyob.easyrental.modules.auth.domain.port.out;

import com.yowyob.easyrental.modules.auth.domain.UserEntity;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Outgoing port for authenticated user lookup.
 *
 * @author Easy Rental Team
 * @since 2026-05-31
 */
public interface AuthUserPort {

    Mono<UserEntity> findByEmail(String email);

    Mono<UserEntity> findById(UUID id);
}
