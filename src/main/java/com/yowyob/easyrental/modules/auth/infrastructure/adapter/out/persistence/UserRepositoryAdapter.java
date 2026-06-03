package com.yowyob.easyrental.modules.auth.infrastructure.adapter.out.persistence;

import com.yowyob.easyrental.modules.auth.domain.UserEntity;
import com.yowyob.easyrental.modules.auth.domain.port.out.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepositoryPort {

    private final UserRepository userRepository;

    @Override
    public Mono<UserEntity> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public Mono<UserEntity> findById(UUID id) {
        return userRepository.findById(id);
    }

    @Override
    public Mono<UserEntity> save(UserEntity user) {
        return userRepository.save(user);
    }

    @Override
    public Mono<Void> clearUserReferences() {
        return userRepository.clearUserReferences();
    }
}
