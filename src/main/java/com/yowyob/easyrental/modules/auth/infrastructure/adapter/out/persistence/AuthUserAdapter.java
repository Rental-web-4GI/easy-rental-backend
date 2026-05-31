package com.yowyob.easyrental.modules.auth.infrastructure.adapter.out.persistence;

import com.yowyob.easyrental.modules.auth.domain.UserEntity;
import com.yowyob.easyrental.modules.auth.domain.port.out.AuthUserPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Adapter implementing AuthUserPort via R2DBC.
 *
 * @author Easy Rental Team
 * @since 2026-05-31
 */
@Component
@RequiredArgsConstructor
public class AuthUserAdapter implements AuthUserPort {

    private final UserRepository userRepository;

    @Override
    public Mono<UserEntity> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public Mono<UserEntity> findById(UUID id) {
        return userRepository.findById(id);
    }
}
